import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class PerfectFailureDetector implements IFailureDetector {
	protected Process p;
    protected Timer t;
    protected HashSet<Integer> suspects;
    protected HashMap<Integer, TimeoutListener> timeoutTimers;
    ScheduledThreadPoolExecutor executor;
	
	// Represents the timeout period for each neighbour
    protected HashMap<Integer, Integer> timeouts;
    protected Integer INITIAL_TIMEOUT;
    protected Integer TIMEOUT_INCR;
	
	// System delay set to average + 2 standard deviations.
	protected final Integer SYSTEM_DELAY = 15; 

	class PeriodicHeartbeat extends TimerTask {
		public void run() {
			p.broadcast("heartbeat", String.format("%d", System.currentTimeMillis()));
		}
	}
	
	
	class TimeoutListener implements Runnable {

		private final Integer pid;
		Future<?> future;
		
		public TimeoutListener(Integer pid) {
			this.pid = pid;
		}
		
		// Action performed after timeout expires, happens only once.
		@Override
		public synchronized void run() {
            suspects.add(pid);
			isSuspected(pid);
		}
		
		public synchronized void startInTime(int timeout){
			future = executor.schedule(TimeoutListener.this, timeout, TimeUnit.MILLISECONDS);
		}
		
		public synchronized void stop(){
			executor.remove(TimeoutListener.this);
			future.cancel(true);
		}
	}
	

	public PerfectFailureDetector(Process p) {
		this.p = p;
		t = new Timer();
		timeoutTimers = new HashMap<Integer, TimeoutListener>();
		suspects = new HashSet<Integer>();
		timeouts = new HashMap<Integer, Integer>();
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(p.n);
		
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
			TimeoutListener oldTimer = timeoutTimers.get(source);
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


	public void isSuspected(Integer pid) {
		Utils.out(p.pid, String.format("P%d has been suspected at %s",
				pid, Utils.timeMillisToDateString(System.currentTimeMillis())));
		if(isSuspect(pid)){
			p.wakeUp();
		}
	}
	
	protected synchronized void removeSuspect(Integer pid) {
		if (suspects.contains(pid)) {
			suspects.remove(pid);
			Utils.out(p.pid, String.format("P%d has been unsuspected at %s",
					pid, Utils.timeMillisToDateString(System.currentTimeMillis())));
		}
	}

	private void startTimeout(Integer process, Integer timeout) {
		TimeoutListener timeoutListener = new TimeoutListener(process);
		timeoutTimers.put(process, timeoutListener);
		timeoutListener.startInTime(timeout);
	}
}
