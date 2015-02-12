
public class SFDProcess extends Process {

	private StrongFailureDetector detector;
	
	private Integer x;
	
	public SFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		x = pid;
		detector = new StrongFailureDetector(this);
	}
	
	public void begin() {
		detector.begin();
		/*
		for r:=1 to n do
			if (i = r) then
				send [VAL: x, r] to all
			if (collect [VAL: v, r] from G[r]) then
				x := v
			decide x // Print message
		*/
		
		
		
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
