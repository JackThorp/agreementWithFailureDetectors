import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

class PerfectFailureDetector implements IFailureDetector {
	private Process p;
	private Timer t;
	private HashSet<Integer> suspects;
	private HashMap<Integer, javax.swing.Timer> timeoutTimers;
	
	// Represents the timeout period for each neighbour
	private HashMap<Integer, Integer> timeouts;
	protected Integer INITIAL_TIMEOUT; 
	protected Integer TIMEOUT_INCR;
	
	// System delay set to average + 2 standard deviations.
	protected final Integer SYSTEM_DELAY = 115; 

	class PeriodicHeartbeat extends TimerTask {
		public void run() {
			p.broadcast("heartbeat", String.format("%d", System.currentTimeMillis()));
		}
	}
	
	
	class TimeoutListener implements ActionListener {

		private final Integer pid;
		
		public TimeoutListener(Integer pid) {
			this.pid = pid;
		}
		
		// Action performed after timeout expires, happens only once.
		@Override
		public synchronized void actionPerformed(ActionEvent e) {
			
			Utils.out(p.pid, String.format("P%d has been suspected at %s",
					pid, Utils.timeMillisToDateString(System.currentTimeMillis())
					));
			
			isSuspected(pid);
		}
	}
	

	public PerfectFailureDetector(Process p) {
		this.p = p;
		t = new Timer();
		timeoutTimers = new HashMap<Integer, javax.swing.Timer>();
		suspects = new HashSet<Integer>();
		timeouts = new HashMap<Integer, Integer>();
		INITIAL_TIMEOUT = Utils.Delta + Utils.DELAY + SYSTEM_DELAY; 
		TIMEOUT_INCR = 0; 
	}

	
	public void begin() {
		t.schedule(new PeriodicHeartbeat(), 0, Utils.Delta);
		
		// Start a timeout for each neighbour
		for (int n_pid = 1; n_pid <= p.n; n_pid++) {
			if (n_pid != p.pid) {
				startTimeout(n_pid, INITIAL_TIMEOUT);
			}
		}
	}

	
	public synchronized void receive(Message m) {

		Integer source = m.getSource();
		
		// If there is a timer running for this process, stop it
		if (timeoutTimers.containsKey(source)) {
			javax.swing.Timer oldTimer = timeoutTimers.get(source);
			oldTimer.stop();
		}
		
		// Get the timeout period for this neighbour
		Integer timeout;
		if(timeouts.containsKey(source)) {
			timeout = timeouts.get(source);	
		} else {
			timeout = INITIAL_TIMEOUT;
			timeouts.put(source, timeout);
		}
		
		
		if (isSuspect(source)) {
			// Falsely suspected, increase the timeout!
			timeout += TIMEOUT_INCR;
			timeouts.put(source, timeout);
			removeSuspect(source);
		}
		
		// Start a new timer
		startTimeout(source, timeout);
	}

	
	public boolean isSuspect(Integer pid) {
		return suspects.contains(pid);
	}


	public synchronized void isSuspected(Integer pid) {
		suspects.add(pid);
		Utils.out(pid, "added " + pid );
		if(isSuspect(pid)){
			p.wakeUp();
		}
	}
	
	
	private synchronized void removeSuspect(Integer pid) {
		if (suspects.contains(pid)) {
			suspects.remove(pid);
			Utils.out(p.pid, String.format("P%d has been unsuspected at %s",
					pid, Utils.timeMillisToDateString(System.currentTimeMillis())));
		}
	}

	
	private void startTimeout(Integer process, Integer timeout) {
		TimeoutListener timeoutListener = new TimeoutListener(process);
		javax.swing.Timer timeoutTimer = new javax.swing.Timer(timeout, timeoutListener);
		timeoutTimer.setRepeats(false);
		timeoutTimers.put(process, timeoutTimer);
		timeoutTimer.start();
	}
}
