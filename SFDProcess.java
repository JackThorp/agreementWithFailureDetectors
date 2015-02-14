import java.util.HashMap;


public class SFDProcess extends Process {

	private StrongFailureDetector detector;
	
	private Integer x;
	private HashMap<Integer, Integer> received;
	
	public SFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		x = pid;
		detector = new StrongFailureDetector(this);
		received = new HashMap<Integer, Integer>();
	}
	
	public void begin() throws InterruptedException {
		detector.begin();
		
		// Loop through rounds, r, and broadcast on your turn
		for(int r = 1; r <= n; r++ ) {
			if(pid == r){
				broadcast("VAL", String.format("%d", x)); 
			} else {
				if(collect(r)) {	
					x = received.get(r);
				}
			}
		}

		Utils.out(pid, String.format("Decided: %d", x));
	}
	
	public synchronized boolean collect(int r) throws InterruptedException {

		while(!received.containsKey(r) && !detector.isSuspect(r)) { 
			wait(); 
		}
			
		// can this value change between breaking out of loop and reaching return statement?
		return !detector.isSuspect(r);
	}
	
	
	public synchronized void receive(Message m) {
		Utils.out(pid, "Received MESSAGE: " + m);
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		} 
		else if (type.equals("VAL")) {
			int v = Integer.parseInt(m.getPayload());
			received.put(m.getSource(), v);
			notifyAll();
		}
	}

	
	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		SFDProcess p = new SFDProcess(name, id, n);
		p.registeR();
		try {
			p.begin();
		}
		catch (InterruptedException e) {}
	}

}
