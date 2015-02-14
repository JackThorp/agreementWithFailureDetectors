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
		Integer r = 0;
		Integer k = 0;
		Integer c = 0;
		while(state == State.UNDECIDED) {
			r++;
			addMesageListToStash(r);
			c = (r % n) + 1;

			Utils.out(pid, "____________________________________");
			Utils.out(pid, "ROUND = " + r + " , COORDINATOR = " + c);
			Message valMessage = new Message(pid, c, "VAL", String.format("%d,%d,%d",estimate, r, k));
			Utils.out(pid, "IM SENDING VAL  =  " + valMessage);
			unicast(valMessage);
			if(pid == c) {
				Utils.out(pid, "IM THE COORDINATOR");
				if(uponReceival(r)){
					estimate = getEstimate(r);
					Utils.out(pid, "IM SENDING THE OUTCOME = " + estimate);
					broadcast("OUTCOME", String.format("%d,%d", estimate, r));
				}
			} else {
				Utils.out(pid, "IM NOT THE COORDINATOR");
				if(collect(c)) {
					Utils.out(pid, "RECEIVED A MESSAGE FROM COORDINATOR");
					String[] payload = outcomes.get(c).getPayload().split(",");
					Utils.out(pid, "ABOUT TO ACK");
					estimate = Integer.parseInt(payload[0]);
					k = Integer.parseInt(payload[1]);
					unicast(new Message(pid, c, "ACK", r.toString()));
					outcomes.remove(c);
				} else {
					Utils.out(pid, "DIDNT RECEIVE A MESSAGE FROM THE COORDINATOR");
					unicast(new Message(pid, c, "NACK", r.toString()));					
				}
					
			}
			
			if(pid == c) {
				if(uponResponse(c)){
					if(acks >= Math.ceil((n / 2))) {
						broadcast("DECISION", String.format("%d,%d", estimate, r));
						Utils.out(pid, String.format("Decided: %d", estimate));
						state = State.DECIDED;
					}
				}
			}
			acks = 0;
			nacks = 0;
		}
		
		
	}
	
	public synchronized boolean collect(int c) throws InterruptedException {
		while(!outcomes.containsKey(c) && !detector.isSuspect(c)) {	wait();	}
		return !detector.isSuspect(c);
	}
	
	public synchronized boolean uponReceival(int r) throws InterruptedException {
		while(stash.get(r).size() < Math.ceil(n/2)){ wait(); }
		return true;
	}
	
	public synchronized boolean uponResponse(int r) throws InterruptedException {
		while(acks < Math.ceil(n/2) && nacks < Math.ceil(n/2)){ wait(); }
		return true;
	}
	
	public synchronized void receive(Message m) {
		if(!m.getType().equals("heartbeat")){
			Utils.out(pid, "Received MESSAGE: " + m);
		}
		
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
				addMesageListToStash(round);
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
	
	private void addMesageListToStash(int round) {
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
