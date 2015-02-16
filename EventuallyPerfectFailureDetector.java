

public class EventuallyPerfectFailureDetector extends PerfectFailureDetector {
	
	public EventuallyPerfectFailureDetector(Process p) {
		super(p);
		INITIAL_TIMEOUT = Utils.Delta  + SYSTEM_DELAY; 
		TIMEOUT_INCR = 30; 
	}
}
