

public class EventuallyPerfectFailureDetector extends PerfectFailureDetector {
	
	
	public EventuallyPerfectFailureDetector(Process p) {
		super(p);
		INITIAL_TIMEOUT = Utils.Delta; 
		TIMEOUT_INCR = 30; 
	}

	
	
	
}
