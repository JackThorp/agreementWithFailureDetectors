import java.util.ArrayList;
import java.util.HashMap;


public class ESFDProcess extends Process {

	enum State {
		DECIDED,
		UNDECIDED
	}
	
	State state;
	private Integer x, acks, nacks;
	//TODO change name of received to "outcomes" ?
	private HashMap<Integer, Message> received;
	private HashMap<Integer, ArrayList<Message>> stash;
	private EventuallyStrongFailureDetector detector;
	
	
	public ESFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		x = pid;
		detector = new EventuallyStrongFailureDetector(this);
		received = new HashMap<Integer, Message>();
		stash = new HashMap<Integer, ArrayList<Message>>();
		state = State.UNDECIDED;
	}
	
	public void begin() throws InterruptedException {
		detector.begin();
		
		Integer estimate = x;
		Integer r = 0;
		Integer k = 0;
		Integer c;
		while(state == State.UNDECIDED) {
			r++;
			c = (r % n) + 1;
			unicast(new Message(pid, c, "VAL", String.format("%d,%d,%d",estimate, r, k)));
			if(pid == c) {
				if(uponReceival(r)){
					estimate = getEstimate(r);
					broadcast("OUTCOME", String.format("%d,%d", estimate, r));
				}
			} else {
				if(collect(c)) {
					String[] payload = received.get(c).getPayload().split(",");
					estimate = Integer.parseInt(payload[0]);
					k = Integer.parseInt(payload[1]);
					unicast(new Message(pid, c, "ACK", r.toString()));
				} else {
					unicast(new Message(pid, c, "NACK", r.toString()));					
				}
					
			}
			
			if(pid == c) {
				if(uponResponse(c)){
					if(acks >= Math.ceil((n+1 / 2))) {
						broadcast("DECISION", String.format("%d,%d", estimate, r));
					}
				}
			}		
		}
	}
	
	private int getEstimate(int r) {
		ArrayList<Message> msgs = stash.get(r);
		int max = 0;
		int estimate = 0;
		for(int i =0; i < msgs.size(); i++) {
			Message m = msgs.get(i);
			String[] payload = m.getPayload().split(",");
			int k = Integer.parseInt(payload[2]);
			if(k > max) {
				max = k;
				estimate = Integer.parseInt(payload[0]); 
			}
		}
		return estimate;
	}
	
	public synchronized boolean collect(int c) throws InterruptedException {

		while(!received.containsKey(c) && !detector.isSuspect(c)) { 
			wait(); 
		}
		
		received.remove(c);
		// can this value change between breaking out of loop and reaching return statement?
		return !detector.isSuspect(c);
	}
	
	public synchronized boolean uponReceival(int r) throws InterruptedException {
		
		while(stash.get(r).size() < Math.ceil((n+1 / 2))){
			wait();
		}
		return false;
	}
	
	public synchronized boolean uponResponse(int r) throws InterruptedException {
		
		while(acks < Math.ceil((n+1 / 2)) || nacks < Math.ceil((n+1 / 2))){
			wait();
		}
		return true;
	}
	
	public synchronized void receive(Message m) {
		Utils.out(pid, "Received MESSAGE: " + m);
		
		switch(m.getType()) {
			case "heartbeat": 
				detector.receive(m);
				break;
			
			case "OUTCOME":
				received.put(m.getSource(), m);
				notifyAll();
				break;
			
			case "VAL":
				Integer round = Integer.parseInt(m.getPayload().split(",")[1]);
				stash.get(round).add(m);
				notifyAll();
				break;
			
			case "ACK":
				acks++;
				notifyAll();
				break;
				
			case "NACK":
				nacks++;
				notifyAll();
				break;
				
			case "DECISION":
				// TODO: Does the round of the message have to be the same as this processes' round?
				if (state == State.UNDECIDED) {
					Integer value = Integer.parseInt(m.getPayload().split(",")[0]);
					Utils.out(pid, String.format("Decided: %d", value));
					state = State.DECIDED;
				}
				
		}

	}
	
	
	
	
	
	
	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		ESFDProcess p = new ESFDProcess(name, id, n);
		p.registeR();
		try {
			p.begin();
		}
		catch (InterruptedException e) {}
	}
	

}
