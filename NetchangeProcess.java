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
	public HashSet<Integer> Neighbours; // note: all autoboxing safe < 128
	
	/* Increments whenever a `mydist` message is received */
	private int mydistCount;
	
	private final int UNDEFINED = -1; 
	
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
			Nbu[v] = null;
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
				Neighbours.add(m.getSource());
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
					recompute(_v);
				}
				Utils.out("CLOSED "+w);
				break;
				
			// on 'opened' - add back to neighbour list and update
			// all local estimates.				
			case Utils.OPENED:
				w = Integer.parseInt(m.getPayload());
				Neighbours.add(w);
				for(int _v = 1; _v <= n; _v++) {
					ndisu[w][_v] = n;
					unicast(new Message(pid, w, "mydist", String.format("%d,%d", _v, Du[_v])));
				}
				Utils.out("OPENNNNNNNNed "+w);
				break;
				
			case "heartbeat":
				Utils.out(pid, String.format("P%d received a heartbeat %s , from %d",
						pid, Utils.timeMillisToDateString(System.currentTimeMillis()), m.getSource()));
				Neighbours.add(m.getSource());
				detector.receive(m);
				break;
				
			default:
				Utils.out("SHOULD NOT HAPPENED");
				break;
		}
		
	}
	
	private void recompute(int v){

		Integer old_v = Du[v];
		
		if(v == pid) {
			Du[v] = 0;
			Nbu[v] = pid;
		}
		// update local estimates if neighbours change
		// improves on current minimum.
		else {
			int best_n = getBestNeighbour(v);
			int d = 1 + ndisu[best_n][v];
			if(d < n) {
				Du[v] = d;
				Nbu[v] = best_n;
			}
			else {
				Du[v] = n;
				Nbu[v] = null;
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
					
					File rt1 = new File(name + "-rt-1.txt");
					
					if(!rt1.exists()){
						rt1.createNewFile();
					}
					
					FileWriter fw = new FileWriter(rt1.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					
					for(int _v = 1; _v <= n; _v++){
						bw.write(String.format("%d:%s\n", _v, p.Nbu[_v]));
					}
					bw.close();
					/*
					 * At this point, you should print your routing table
					 * (not distances) to a file named `name`-rt-1.txt.
					 *
					 * The format should be along the lines of:
					 * String.format("%d:%s")
					 * where:
					 * %d is the destination id, and
					 * %s is the string representation of the next best hop.
					 * This can be "local", "undefined", or a number.
					 */
					
					/* Reset counters */
					previous = 0;
				} else {
					previous = current;
				}
			
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
}

