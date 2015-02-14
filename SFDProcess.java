import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SFDProcess extends Process {

	private StrongFailureDetector detector;
	
	
	private Integer x;
	private Integer r;
	private Integer v;
	private HashMap<Integer, Integer> received;
	
    private final Lock lock = new ReentrantLock();

    public final Condition notCollected = lock.newCondition();
	
	
	public SFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		x = pid;
		detector = new StrongFailureDetector(this);
		received = new HashMap<Integer, Integer>();
	}
	
	public void begin() throws InterruptedException {
		detector.begin();

		Utils.out(pid, String.format("My pid is %s", x)); 
		// Loop through rounds, r, and broadcast on your turn
		for(r = 1; r <= n; r++ ) {
			if(pid == r){
				broadcast("VAL", String.format("%d",x)); 
			} else {
				if(collect(r)) {	
					x = v; // update decision on successful collection.
				}
			}
		}

		Utils.out(pid, String.format("Decided: %d", x));
	}
	
	public synchronized boolean collect(int r) throws InterruptedException {

		while(!received.containsKey(r) && !detector.isSuspect(r)) { 
			Utils.out(pid, String.format("WAITING IN COLLECT() FOR %d", r));
			wait(); 
		}
		
		//received.remove(r);
		notifyAll();
	
		// can this value change between breaking out of loop and reaching return statement?
		return !detector.isSuspect(r);
	}
	
	
	public void receive(Message m) {
		Utils.out(pid, "RECEIVED A MESSAGE of type = " + m.getType());
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		} else if (type.equals("VAL")) {
			Utils.out(pid, "MSG:" + m.toString());
			v = Integer.parseInt(m.getPayload()); //should be pid value?
			received.put(m.getSource(), v);
			wakeUp();
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
		catch (InterruptedException e) {
			
		}
	}

}
