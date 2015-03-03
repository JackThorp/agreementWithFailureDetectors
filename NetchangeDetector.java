import java.util.HashSet;


public class NetchangeDetector extends PerfectFailureDetector {

	public HashSet<Integer> Neighbours; // note: all autoboxing safe < 128
	
	public NetchangeDetector(Process p) {
		super(p);
		Neighbours = new HashSet<>(); 
	}

	@Override
	public synchronized void receive(Message m) {
		Neighbours.add(m.getSource());
		super.receive(m);
	}

	@Override
	public void isSuspected(Integer pid) {
		super.isSuspected(pid);
		Neighbours.remove(pid);
	}

	public void removeNeighbour(int w) {
		Neighbours.remove(w);
	}

	public void addNeighbour(int w) {
		Neighbours.add(w);
	}
	
	
	

}
