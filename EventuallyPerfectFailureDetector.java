import java.util.HashMap;



public class EventuallyPerfectFailureDetector extends PerfectFailureDetector {

	// Represents the timeout period for each neighbour
	HashMap<Integer, Integer> timeouts;
	private final Integer INITIAL_TIMEOUT = Utils.Delta; 
	private final Integer TIMEOUT_INCR = 100; // TODO: What value should this be?
	
	public EventuallyPerfectFailureDetector(Process p) {
		super(p);
		timeouts = new HashMap<Integer, Integer>();
	}

	
	@Override
	public void receive(Message m) {
		
		Utils.out(p.pid, m.toString());	
		Integer source = m.getSource();
		
		// If there is a timer running for this process, stop it
		if (timeoutTimers.containsKey(source)) {
			javax.swing.Timer oldTimer = timeoutTimers.get(source);
			oldTimer.stop();
		}
		
		// Get the timeout for this process, if none is found, add the initial_timeout
		Integer timeout;
		if (timeouts.containsKey(source)) {
			timeout = timeouts.get(source);
		}
		else {
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
		TimeoutListener timeoutListener = new TimeoutListener(source);
		javax.swing.Timer timeoutTimer = new javax.swing.Timer(timeout, timeoutListener);
		timeoutTimer.setRepeats(false);
		timeoutTimers.put(source, timeoutTimer);
		timeoutTimer.start();
	}
	
	
	
}
