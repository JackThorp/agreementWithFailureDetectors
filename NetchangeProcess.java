import java.util.HashSet;
import java.util.Iterator;


public class NetchangeProcess extends Process {

	private NetchangeDetector detector;
	private Integer[] Du;				//estimated d(u,v), u=>pid
	private Integer[] Nbu; 				//routing table
	private Integer[][] ndisu;			//estimated d(w,v)
	public HashSet<Integer> Neighbours; // note: all autoboxing safe < 128

//	Initialisation
//	D_u [N]
//	Nb_u [N]
//	ndis_u [N][N]
//	Neighbours_u = {}
	public NetchangeProcess(String name, int pid, int n) {
		super(name, pid, n);
		detector = new NetchangeDetector(this);
		Du = new Integer[n + 1]; 		// processes start at 1 so we leave [0] blank
		Nbu = new Integer[n + 1];		// Routing table
		ndisu = new Integer[n+1][n+1];	// Neighbour w's estimated distance to v ndisu[w][v] 
	}

	public void begin() {
		detector.begin();
		
//		forall w, v in V do
//		ndis_u [w][v] = N
		for (int w = 1; w <= n; w++) {
			for (int v = 1; v <= n; v++) {
				ndisu[w][v] = n;
			}
		}
		
//		forall v in V do
//		D_u [v] = N
//		Nb_u [v] = undefined
		for (int v = 1; v <= n; v++) {
			Du[v] = n;
			Nbu[v] = null;
		}
		
//		D_u [u] = 0
//		Nb u [u] = local
		Du[pid] = 0;
		Nbu[pid] = pid;
		
//		forall w in Neighbors_u do
//		send [mydist: u, 0] to w
		broadcast("mydist", String.format("%d,%d", pid, 0));
	}

	@Override
	public synchronized void receive(Message m) {
		super.receive(m); // Util.out msg
		int w;
		switch(m.getType()) {
			case "mydist":
//				upon receipt of [mydist: v, d] from w
//				ndisu [w][v] = d
//				Recompute(v)
				Neighbours.add(m.getSource());
				int v = Integer.parseInt(m.getPayload().split(",")[0]);
				int d = Integer.parseInt(m.getPayload().split(",")[1]);
				w = m.getSource();
				ndisu[w][v] = d;
				recompute(v);
				break;

			case "CLOSED":
//				upon receipt of [closed: w]
//				Neighborsu = Neighborsu \ { w }
//				forall v in V do
//				Recompute(v)	
				w = Integer.parseInt(m.getPayload());
				Neighbours.remove(w);
				for (int _v = 1; _v <= n; _v++) {
					recompute(_v);
				}
				break;
				
			case "OPENED":
//				upon receipt of [open: w]
//				Neighbors u = Neighbors u [ {w}
//				forall v 2 V do
//				ndis u [w][v] = n
//				send [mydist: v, D u [v]] to w
				w = Integer.parseInt(m.getPayload());
				Neighbours.add(w);
				for(int _v = 1; _v <= n; n++) {
					ndisu[w][_v] = n;
					unicast(new Message(pid, w, "mydist", String.format("%d,%d", _v, Du[_v])));
				}
				break;
				
			case "heartbeat":
				Neighbours.add(m.getSource());
				detector.receive(m);
				int nei = m.getSource();
				break;
		}
	}
	
	private void recompute(int v){
//		if v = u then
//		D u [v] = 0
//		Nb u [v] = local
		int old_v = Du[v];
		if(v == pid) {
			Du[v] = 0;
			Nbu[v] = pid;
		}
//		else
//		d = 1 + min{ ndis u [w][v] }
//		such that w 2 Neighbors u
//		if d < n then
//		D u [v] = d
//		Nb u [v] = w such that
//		( 1 + ndis u [w][v]) = d
		else {
			int best_n = getBestNeighbour(v);
			int d = 1 + ndisu[best_n][v];
			if(d < n) {
				Du[v] = d;
				Nbu[v] = best_n;
			}
//			else
//			D u [v] = N
//			Nb u [v] = undefined
			else {
				Du[v] = n;
				Nbu[v] = null;
			}
		}
//		if D u [v] has changed then
//		forall w 2 Neighbors u do
//		send [mydist: v, D u [v]] to w
		if (Du[v] != old_v) {
			broadcast("mydist", String.format("%d,%d", v, Du[v]));
		}
	}

	private int getBestNeighbour(int v) {
		int nbr = (int) Neighbours.toArray()[0];
		int min_dist = ndisu[nbr][v];
		for(int w : Neighbours) {
			if(ndisu[w][v] < min_dist) {
				nbr = w;
				min_dist = ndisu[w][v];
			}
		}
		return nbr;
	}

	public static void main(String [] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		NetchangeProcess p = new NetchangeProcess(name, id, n);
		p.registeR();
		p.begin();
	}

}
