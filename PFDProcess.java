
public class PFDProcess extends Process {
	
	private PerfectFailureDetector detector;
	
	public PFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		detector = new PerfectFailureDetector(this);
	}

	
	public void begin() {
		detector.begin();
	}
	
	
	public synchronized void receive(Message m) {
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		}
	}

	
	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		PFDProcess p = new PFDProcess(name, id, n);
		p.registeR();
		p.begin();
	}
	
}
