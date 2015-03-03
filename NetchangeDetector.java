public class NetchangeDetector extends PerfectFailureDetector {
	
	public NetchangeDetector(Process p) {
		super(p);
	}

	@Override
	public void isSuspected(Integer pid) {
		super.isSuspected(pid);
		p.receive(new Message(p.pid, p.pid, "CLOSED", String.format("%d", pid)));
	}

	@Override
	protected synchronized void removeSuspect(Integer pid) {
		super.removeSuspect(pid);
		p.receive(new Message(p.pid, p.pid, "OPENED", String.format("%d", pid)));
	}
}
