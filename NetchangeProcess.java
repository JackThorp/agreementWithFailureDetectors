import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;

/* Copyright (c) 2013-2015, Imperial College London
 * All rights reserved.
 *
 * Distributed Algorithms, CO347
 */

class NetchangeProcess extends Process {
	
	/* This array stores the minimum hop-count 
	 * distance to a destination `v` in [1,n] 
	 */
	private NetchangeDetector detector;
	private Integer[] Du;				//estimated d(u,v), u=>pid
	private Integer[] Nbu; 				//routing table
	private Integer[][] ndisu;			//estimated d(w,v)
	private HashSet<Integer> Neighbours; // note: all autoboxing safe < 128	
	
	/* Increments whenever a `mydist` message is received */
	private int mydistCount;
	
	private final static int UNDEFINED = -1; 
	
	public NetchangeProcess (String name, int pid, int n) {
		super(name, pid, n);	
		mydistCount = 0;
		
		detector = new NetchangeDetector(this);
		Du = new Integer[n + 1]; 		// processes start at 1 so we leave [0] blank
		Nbu = new Integer[n + 1];		// Routing table
		ndisu = new Integer[n+1][n+1];	// Neighbour w's estimated distance to v ndisu[w][v]
		Neighbours = new HashSet<>(); // note: all autoboxing safe < 128

	}
	
	public void begin () {
		detector.begin();
		
		// initialise neighbour estimates to n
		for (int w = 1; w <= n; w++) {
			for (int v = 1; v <= n; v++) {
				ndisu[w][v] = n;
			}
		}
		
		// Set all local estimates to n & initialise routing table
		for (int v = 1; v <= n; v++) {
			Du[v] = n;
			Nbu[v] = UNDEFINED;
		}
		
		Du[pid] = 0;
		Nbu[pid] = pid;
	
		broadcast("mydist", String.format("%d,%d", pid, 0));	
	}
	
	public void checkRoutingDistances () {
		
		/* It creates a message of the form:
		 * this.pid<|>0<|>Utils.CHECK_COST<|>1:D[1];2:D[2]...n:D[n]
		 */
		String payload = "";
		String f;
		for (int v = 1; v < n + 1; v++) {
			f = "%d:%d";
			if (v != n) f += ";";
			payload += String.format(f, v, Du[v]);
		}
		Message m = new Message(pid, 0, Utils.CHECK_COST, payload);
		if (unicast (m))
			Utils.out(pid, "OK");
		else
			Utils.out(pid, "Error");
		return ;
	}
	
	public int getMydistCount() {
		return mydistCount;
	}
	
	@Override
	public synchronized void receive (Message m) {
//		Utils.out(pid, m.toString());

		int w;
		switch(m.getType()) {
			// on 'mydist' - update neighbour estimates and recompute local estimate.
			case "mydist":
				mydistCount++;
				addNeighbours(m.getSource());
				int v = Integer.parseInt(m.getPayload().split(",")[0]);
				int d = Integer.parseInt(m.getPayload().split(",")[1]);
				w = m.getSource();
				ndisu[w][v] = d;
				recompute(v);
				break;

			// on 'close' - remove from neighbours and recompute 
			// local estimates to all destiantions
			case Utils.CLOSED:
				w = Integer.parseInt(m.getPayload());
				Neighbours.remove(w);
				for (int _v = 1; _v <= n; _v++) {
//					if(Neighbours.contains(_v)){
						recompute(_v);
//					}	
				}
				Utils.out("CLOSED "+w+" and neighbours are now "+Neighbours.toString());
				break;
				
			// on 'opened' - add back to neighbour list and update
			// all local estimates.				
			case Utils.OPENED:
				w = Integer.parseInt(m.getPayload());
				addNeighbours(w);
				for(int _v = 1; _v <= n; _v++) {
					ndisu[w][_v] = n;
					unicast(new Message(pid, w, "mydist", String.format("%d,%d", _v, Du[_v])));
				}
				Utils.out("OPENNNNNNNNed "+w);
				break;
				
			case "heartbeat":
				addNeighbours(m.getSource());
				detector.receive(m);
				break;
				
			default:
				Utils.out("SHOULD NOT HAPPEN");
				break;
		}
	}
	
	private void addNeighbours(int w){
		if (w!=pid) Neighbours.add(w);
	}
	
	private void recompute(int v){

		Integer old_v = Du[v];
		// update local estimates if neighbours change
		// improves on current minimum.
//		if (v == pid) {
//			Du[v] = 0;
//			Nbu[v] = pid;
//		}else{
		if (v != pid ) {
			int best_n = getBestNeighbour(v);
			int d = 1 + ndisu[best_n][v];
			Utils.out("Best Neighbour for "+v+" is "+best_n+" with distance "+d);
			if(d < n) {
				Du[v] = d;
				Nbu[v] = best_n;
			}
			else {
				Du[v] = n;
				Nbu[v] = UNDEFINED;
			}
		}

		// if local estimate has changed, broadcast to neighbours
		if (!Du[v].equals(old_v)) {
			broadcast("mydist", String.format("%d,%d", v, Du[v]));
		}
	}

	private int getBestNeighbour(int v) {
		int nbr = UNDEFINED;
		int min_dist = Integer.MAX_VALUE;
		for(int w : Neighbours) {
			if(ndisu[w][v] < min_dist) {
				nbr = w;
				min_dist = ndisu[w][v];
			}
		}
		return nbr;
	}
	
	public static void main (String [] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int  n = Integer.parseInt(args[2]);
		NetchangeProcess p = new NetchangeProcess(name, id, n);
		p.registeR ();
		p.begin ();
		
		/* Check periodically for convergence. */
		int current, previous = 0;
		int count = 0;
		
		while (true) { /* Sleep, poll, check. */
			try {
				/* Follow the periodicity of heartbeat messages */
				Thread.sleep(Utils.Delta);
				/* Get the current `mydist` message count */
				current = p.getMydistCount();
				
				/* For debugging purposes */
				Utils.out(id, 
				String.format("previous = %d, current = %d, count = %d", 
				previous, current, count));
				
				if (previous == current) count ++;
				else count = 0;
				
				/* 10 x Delta should be enough. */
				if (count == 10) {
					/* Check computed routing distances */
					p.checkRoutingDistances();
					
					File rt1 = new File(name + "-rt-1.out");
					
					if(!rt1.exists()){
						rt1.createNewFile();
					}
					
					FileWriter fw = new FileWriter(rt1.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					
					for(int _v = 1; _v <= n; _v++){
						String s = p.Nbu[_v].toString();
						if (p.Nbu[_v] == p.pid){
							s = "local";
						}else if(p.Nbu[_v].equals(UNDEFINED)){
							s = "undefined";
						}
						bw.write(String.format("%d:%s\n", _v, s));
					}
					bw.close();

					previous = 0;
				} else {
					previous = current;
				}
			
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
}

