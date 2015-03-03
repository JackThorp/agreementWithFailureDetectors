import java.util.HashSet;
import java.util.Iterator;


public class NetchangeProcess extends Process {

	private NetchangeDetector detector;
	private Integer[] Du;				//estimated d(u,v), u=>pid
	private Integer[] Nbu; 				//routing table
	private Integer[][] ndisu;			//estimated d(w,v)
	private HashSet<Integer> Neighbours; // note: all autoboxing safe < 128
	private static int local = 999999; 	//TODO: what is local??

//	Initialisation
//	D_u [N]
//	Nb_u [N]
//	ndis_u [N][N]
//	Neighbours_u = {}
	public NetchangeProcess(String name, int pid, int n) {
		super(name, pid, n);
		detector = new NetchangeDetector(this);
		Du = new Integer[n + 1]; // processes start at 1 so we leave [0] blank
		Nbu = new Integer[n + 1];
		ndisu = new Integer[n+1][n+1];
		Neighbours = new HashSet<>();
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
		Nbu[pid] = local;
		
//		forall w in Neighbors_u do
//		send [mydist: u, 0] to w
//		TODO: or broadcast! ??
		for (Integer w : Neighbours) {
			unicast(new Message(pid, w, "mydist", String.format("%d,%d", pid, 0)));
		}
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
//				for (int v = 0; v < n; v++) {
//					
//				}
				break;
				
			case "OPENED":
//				upon receipt of [open: w]
//				Neighbors u = Neighbors u [ {w}
//				forall v 2 V do
//				ndis u [w][v] = n
//				send [mydist: v, D u [v]] to w
				w = Integer.parseInt(m.getPayload());
				break;
			default: // heartbeats 
				detector.receive(m);
				break;
		}
	}
	
	private void recompute(int v){
//		if v = u then
//		D u [v] = 0
//		Nb u [v] = local
//		else
//		d = 1 + min{ ndis u [w][v] }
//		such that w 2 Neighbors u
//		if d < n then
//		D u [v] = d
//		Nb u [v] = w such that
//		( 1 + ndis u [w][v]) = d
//		else
//		D u [v] = N
//		Nb u [v] = undefined
//		if D u [v] has changed then
//		forall w 2 Neighbors u do
//		send [mydist: v, D u [v]] to w
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
