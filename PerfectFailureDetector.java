import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

class PerfectFailureDetector implements IFailureDetector {
	Process p;
	Timer t;
	HashSet<Integer> suspects;
	HashMap<Integer, javax.swing.Timer> timeoutTimers;

	class PeriodicHeartbeat extends TimerTask {
		public void run() {
			p.broadcast("heartbeat", String.format("%d", System.currentTimeMillis()));
			Utils.out(p.pid, "***"+suspects.toString());
		}
	}
	
	class TimeoutListener implements ActionListener {

		private final Integer pid;
		
		public TimeoutListener(Integer pid) {
			this.pid = pid;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			suspects.add(pid);
			Utils.out(p.pid, String.format("P%d has been [un]suspected at %s",
					pid, Utils.timeMillisToDateString(System.currentTimeMillis())));
		}
	}
	

	public PerfectFailureDetector(Process p) {
		this.p = p;
		t = new Timer();
		timeoutTimers = new HashMap<Integer, javax.swing.Timer>();
		suspects = new HashSet<Integer>();
	}

	public void begin() {
		t.schedule(new PeriodicHeartbeat(), 0, Utils.Delta);
	}

	public void receive(Message m) {
		Utils.out(p.pid, m.toString());	
		
		// Remove the process from suspects
		Integer source = m.getSource();
		suspects.remove(source);
		
		// If there is a timer running for this process, stop it
		if (timeoutTimers.containsKey(source)) {
			javax.swing.Timer oldTimer = timeoutTimers.get(source);
			oldTimer.stop();
		}
		
		// Start a new timer
		TimeoutListener timeoutListener = new TimeoutListener(source);
		javax.swing.Timer timeoutTimer = 
				new javax.swing.Timer(Utils.DELAY + Utils.Delta, timeoutListener);
		timeoutTimer.setRepeats(false);
		timeoutTimers.put(source, timeoutTimer);
		timeoutTimer.start();
	}

	public boolean isSuspect(Integer pid) {
		return suspects.contains(pid);
	}

	public void isSuspected(Integer process) {
		return;
	}

	
	
}
