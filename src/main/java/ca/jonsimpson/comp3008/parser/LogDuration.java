package ca.jonsimpson.comp3008.parser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An easy way to determine the duration between successes and failures. Handles
 * receiving start, success, and fail messages in any order. <br>
 * When {@link LogDuration#success(Object[])} or
 * {@link LogDuration#fail(Object[])} is called, they make sure that start was
 * called once before, gets the number of seconds between the start and itself,
 * then creates a log message of the following format:
 * <code>user type duration</code>. Where user is the user as specified by the
 * constructor, type being either <code>success</code> or <code>failure</code>,
 * and duration being the time it took in seconds.
 */
public class LogDuration {
	
	public static final int USER = 0;
	public static final int TYPE = 1;
	public static final int DURATION = 2;
	
	private String user;
	private Date start;
	private List<Object[]> logs = new ArrayList<>();
	
	public LogDuration(String user) {
		this.user = user;
	}
	
	public void start(Object[] row) {
		start = (Date) row[DataParser.DATE];
	}
	
	public void success(Object[] row) {
		recordDuration(row, "success");
	}
	
	public void fail(Object[] row) {
		recordDuration(row, "failure");
	}
	
	private void recordDuration(Object[] row, String type) {
		if (start == null) {
			return;
		}
		Date end = (Date) row[DataParser.DATE];
		long duration = end.getTime() - start.getTime();
		// convert milliseconds to seconds since the dataset is in seconds
		duration /= 1000;
		logs.add(new Object[] { user, type, duration });
		reset();
	}
	
	private void reset() {
		start = null;
	}
	
	public List<Object[]> getLogs() {
		return logs;
	}
	
}