import java.util.HashSet;
import java.util.Iterator;


public class NetchangeProcess extends Process {

	private NetchangeDetector detector;
	private Integer[] D;
	private Integer[] Nb; 				//routing table
	private Integer[][] ndis;
	private HashSet<Integer> Neighbours;
	private static int local = 999999; 	//TODO: what is local??

//	Initialisation
//	D_u [N]
//	Nb_u [N]
//	ndis_u [N][N]
//	Neighbours_u = {}
	public NetchangeProcess(String name, int pid, int n) {
		super(name, pid, n);
		detector = new NetchangeDetector(this);
		D = new Integer[n + 1]; // processes start at 1 so we leave [0] blank
		Nb = new Integer[n + 1];
		ndis = new Integer[n+1][n+1];
		Neighbours = new HashSet<>();
	}

	public void begin() {
		detector.begin();
		
//		forall w, v in V do
//		ndis_u [w][v] = N
		for (int w = 1; w <= n; w++) {
			for (int v = 1; v <= n; v++) {
				ndis[w][v] = n;
			}
		}
		
//		forall v in V do
//		D_u [v] = N
//		Nb_u [v] = undefined
		for (int v = 1; v <= n; v++) {
			D[v] = n;
			Nb[v] = null;
		}
		
//		D_u [u] = 0
//		Nb u [u] = local
		D[pid] = 0;
		Nb[pid] = local;
		
//		forall w in Neighbors_u do
//		send [mydist: u, 0] to w
//		TODO: or broadcast! ??
		for (Integer w : Neighbours) {
			unicast(new Message(pid, w, "mydist", String.format("%d,%d", pid, 0)));
		}
	}
	


	@Override
	public synchronized void receive(Message m) {
		// TODO Auto-generated method stub
		super.receive(m);
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
