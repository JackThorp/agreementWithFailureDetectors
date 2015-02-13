import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SFDProcess extends Process {

	private StrongFailureDetector detector;
	
	
	private Integer x;
	private Integer r;
	private Integer v;
	private boolean received;
	
    private final Lock lock = new ReentrantLock();

    public final Condition notCollected = lock.newCondition();
	
	
	public SFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		x = pid;
		detector = new StrongFailureDetector(this);
		received = false;
	}
	
	public void begin() throws InterruptedException {
		detector.begin();

		Utils.out(pid, String.format("My pid is %s", x)); 
		// TODO: solve fact that pids are not like 1, 2, 3 etc so this will not work ??
		// Loop through rounds, r, and broadcast on your turn
		for(r = 1; r < n; n++ ) {
			if(pid == r){
				Utils.out(pid, "BROADCASTING A VALUE"); 
				broadcast("VAL", String.format("%d",x)); // if your turn broadcast your decision, x. 
			}
			
			// collect r will block until a receive event or suspicion
			if(collect(r)) {	
				Utils.out(pid, "RECEIVED A VALUE"); 
				x = v; // update decision on successful collection.
			}
			else {
				Utils.out(pid, "DIDNT RECEIVE A VALUE"); 
			}
		}

		
	}
	
	public synchronized boolean collect(int r) throws InterruptedException {
		/*
		while(!received && !detector.isSuspect(r)) { 
			Utils.out(pid, "WAITING IN COLLECT()");
			wait(); 
		}
		Utils.out(pid, "GOT OUT YO ");

		received = false;
		notifyAll(); 
	
		// can this value change between breaking out of loop and reaching return statement?
		 * 
		 */
		lock.lock();

        try {
			while(!received && !detector.isSuspect(r)){
				Utils.out(pid, "WAITING AGAIN:(");
				notCollected.await();
				Utils.out(pid, "STOPPED WAITING:)");
			}
			if (received) {
				Utils.out(pid, "RECEIVED YOOOOO");
			}
			else {
				Utils.out(pid, "SUSPECTED YOOOOO");
			}
        }
        finally {
        	lock.unlock();
        }
			
		return !detector.isSuspect(r);
	}
	
	
	public void receive(Message m) {
		Utils.out(pid, "RECEIVED A MESSAGE of type = " + m.getType());
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		} else if (type.equals("VAL")) {
			if(m.getSource() == r) {
				v = Integer.parseInt(m.getPayload()); //should be pid value?
				received = true;
				notCollected.signalAll();
			}
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
	
	public void signalCondition() {
		notCollected.signalAll();
	}
	

}
