public class NetchangeDetector extends PerfectFailureDetector {
	
	public NetchangeDetector(Process p) {
		super(p);
		INITIAL_TIMEOUT = 2*(Utils.Delta + Utils.DELAY + SYSTEM_DELAY);
		TIMEOUT_INCR = 30; 
	}

	@Override
	public void isSuspected(Integer pid) {
		super.isSuspected(pid);
		p.receive(new Message(p.pid, p.pid, Utils.CLOSED, String.format("%d", pid)));
	}

	@Override
	protected synchronized void removeSuspect(Integer pid) {
		super.removeSuspect(pid);
		p.receive(new Message(p.pid, p.pid, Utils.OPENED, String.format("%d", pid)));
	}
}
