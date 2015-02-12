public class SFDProcess extends Process {

	private StrongFailureDetector detector;
	
	private Integer x;
	private Integer r;
	private Integer v;
	private boolean received;
	
	public SFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		x = pid;
		detector = new StrongFailureDetector(this);
		received = false;
	}
	
	public void begin() throws InterruptedException {
		detector.begin();
		
		// TODO: solve fact that pids are not like 1, 2, 3 etc so this will not work ??
		// Loop through rounds, r, and broadcast on your turn  
		for(r = 1; r < n; n++ ) {
			if(pid == r){
				broadcast("VAL", String.format("%d",x)); // if your turn broadcast your decision, x. 
			}
			
			// collect r will block until a receive event or suspicion
			if(collect(r)) {	
				x = v; // update decision on successful collection.
			}
		}	
		
	}
	
	public synchronized boolean collect(int r) throws InterruptedException {
		while(!received && !detector.isSuspect(r)) { 
			wait(); 
		}
		
		received = false;
		notifyAll();
	
		// can this value change between breaking out of loop and reaching return statement?
		return !detector.isSuspect(r);
	}
	
	
	public void receive(Message m) {
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		} else if (type.equals("VAL")) {
			if(m.getSource() == r) {
				v = Integer.parseInt(m.getPayload()); //should be pid value?
				received = true;
			}
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
