import java.util.ArrayList;
import java.util.HashMap;


public class ESFDProcess extends Process {

	enum State {
		DECIDED,
		UNDECIDED
	}
	
	State state;
	private Integer x, acks, nacks;
	private HashMap<Integer, Message> outcomes;
	private HashMap<Integer, ArrayList<Message>> stash;
	private EventuallyStrongFailureDetector detector;
	
	
	public ESFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		x = pid;
		detector = new EventuallyStrongFailureDetector(this);
		outcomes = new HashMap<Integer, Message>();
		stash = new HashMap<Integer, ArrayList<Message>>();
		state = State.UNDECIDED;
		acks = 0;
		nacks = 0;
	}
	
	public synchronized void begin() throws InterruptedException {
		detector.begin();
		
		Integer estimate = x;
		// Round number(k), last time OUTCOME received(k), coordinator(c)
		Integer r = 0;
		Integer k = 0;
		Integer c = 0;
		while(state == State.UNDECIDED) {
			r++;
			createListForRound(r);
			
			// Get the coordinator for this round
			c = (r % n) + 1;
			boolean isCoordinator = pid == c;
			Utils.out(pid, "___________ ROUND: " + r + " , COORD: " + c + " ___________");
			// Send our estimate to the coordinator
			Message valMessage = new Message(pid, c, "VAL", String.format("%d,%d,%d", estimate, r, k));
			unicast(valMessage);
			Utils.out(pid, "Sent VAL");
			
			if(isCoordinator) {
				if(collectVals(r)){
					estimate = getEstimate(r);
					broadcast("OUTCOME", String.format("%d,%d", estimate, r));
					Utils.out(pid, "Sent OUTCOME");
				}
			} 
			else if(collectOutcome(c)) {
				String[] payload = outcomes.get(c).getPayload().split(",");
				estimate = Integer.parseInt(payload[0]);
				k = Integer.parseInt(payload[1]);
				unicast(new Message(pid, c, "ACK", r.toString()));
				outcomes.remove(c);
			} 
			else {
				unicast(new Message(pid, c, "NACK", r.toString()));					
			}
					
			
			if(isCoordinator && collectAcks(c)){
				if(acks >= Math.ceil((n / 2))) {
					broadcast("DECISION", String.format("%d,%d", estimate, r));
					Utils.out(pid, String.format("Decided: %d", estimate));
					state = State.DECIDED;
				}
			}
			acks = 0;
			nacks = 0;
		}
	}
	
	
	
	public synchronized boolean collectOutcome(int c) throws InterruptedException {
		while(!outcomes.containsKey(c) && !detector.isSuspect(c)) {	
			Utils.out(pid, "Waiting to collect outcome.");
			wait();
		}
		Utils.out(pid, "Stopped waiting to collect outcome.");
		return !detector.isSuspect(c);
	}
	
	public synchronized boolean collectVals(int r) throws InterruptedException {
		while(stash.get(r).size() < Math.ceil(n/2)){ wait(); }
		return true;
	}
	
	public synchronized boolean collectAcks(int r) throws InterruptedException {
		while(acks < Math.ceil(n/2) && nacks < Math.ceil(n/2)){ wait(); }
		return true;
	}
	
	public synchronized void receive(Message m) {
		
		switch(m.getType()) {
			case "heartbeat": 
				detector.receive(m);
				break;
			
			case "OUTCOME":
				outcomes.put(m.getSource(), m);
				notifyAll();
				break;
			
			case "VAL":
				Integer round = Integer.parseInt(m.getPayload().split(",")[1]);
				createListForRound(round);
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
				if (state == State.UNDECIDED) {
					Integer value = Integer.parseInt(m.getPayload().split(",")[0]);
					state = State.DECIDED;
					Utils.out(pid, String.format("Decided: %d", value));
				}
				
		}

	}
	
	
	/* Takes care of creating an empty list in stash for each round */
	private void createListForRound(int round) {
		if (!stash.containsKey(round)) {
			ArrayList<Message> msgs = new ArrayList<Message>();
			stash.put(round, msgs);
		}
	}
	
	
	private int getEstimate(int r) {
		ArrayList<Message> msgs = stash.get(r);
		int max = -1;
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
