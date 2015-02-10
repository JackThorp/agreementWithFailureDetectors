import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

class PerfectFailureDetector implements IFailureDetector {
	Process p;
	LinkedList<Integer> suspects;
	Timer t;
	static final int Delta = 1000; /* 1sec */
	HashMap<Integer, Boolean> registered = new HashMap<Integer, Boolean>();

	class PeriodicTask extends TimerTask {
		public void run() {
			p.broadcast("heartbeat", String.format("%d", System.currentTimeMillis()));
		}
	}
	
	class TimeOutTask extends TimerTask {

		@Override
		public void run() {
			Iterator<Integer> it = registered.keySet().iterator();
			while(it.hasNext()) {
				Integer key = it.next();
				if(!registered.get(key)){
					suspects.add(key);
				}
				registered.put(key, false);
			}			
		}
		
	}

	public PerfectFailureDetector(Process p) {
		this.p = p;
		t = new Timer();
		suspects = new LinkedList<Integer>();
	}

	public void begin() {
		// set all hashmap values to true
		t.schedule(new PeriodicTask(), 0, Delta);
	}

	public void receive(Message m) {
		Utils.out(p.pid, m.toString());
		
		if()
		registered.put(p.pid, true);
	}

	public boolean isSuspect(Integer pid) {
		return suspects.contains(pid);
	}

	public void isSuspected(Integer process) {
		return;
	}

}
