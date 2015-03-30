package ca.jonsimpson.comp3008.parser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;

/**
 * Read csv files containing the logs of password login schemes
 * 
 * Here are the columns from the csv files. The data column may contain commas,
 * which is why this script groups everything after 7 columns into the data one.
 * <code>time	site	user	scheme	mode	event	data</code>
 * <br>
 * Uses arrays instead of instance objects since arrays are blazingly fast.
 */
public class DataParser {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	// specify the index positions of the elements for each Object[] in data
	public static final int DATE = 0;
	public static final int SITE = 1;
	public static final int USER = 2;
	public static final int SCHEME = 3;
	public static final int MODE = 4;
	public static final int EVENT = 5;
	public static final int DATA = 6;
	
	// list of all log entries
	private List<Object[]> data = new ArrayList<>();
	
	// track usernames
	private Set<String> users = new TreeSet<>();
	
	// track login counts for all users
	private Map<String, Integer[]> loginCountResults = new HashMap<>();
	
	// track login durations for all users
	private List<Object[]> loginDurationResults = new ArrayList<>();
	
	public DataParser(String filename) throws Exception {
		
		// parse the csv file into a list of object arrays
		parseCSVFile(filename);
		
		// ensure the logfiles are in ascending order by its timestamp
		sortDataByDate();
		
		// record counts and durations for each login event
		calculateDurations();
	}

	private void calculateDurations() {
		// iterate over each user, finding their logs
		for (String user: users) {
			
			// get all logs for this user
			List<Object[]> userLogs = getLogsByUser(user);
			
			// track start, success and failure counts
			int loginStart = 0;
			int loginSuccess = 0;
			int loginFailure = 0;
			
			// track durations between actions
			LogDuration ld = new LogDuration(user);
			
			// for each log, determine what kind it is
			for (Object[] row : userLogs) {
				String mode = (String) row[MODE];
				String event = (String) row[EVENT];
				
				// compare mode and event columns
				switch (mode) {
				case "create":
					switch (event) {
					case "start":
						break;
					case "passwordSubmitted":
						break;
					default:
						break;
					}
					break;
				
				case "enter":
					switch (event) {
					case "passwordSubmitted":
						break;
					case "start":
						
						break;
					default:
						break;
					}
					break;
				
					// logins are what we're looking for
				case "login":
					switch (event) {
					case "start":
						ld.start(row);
						break;
					case "success":
						loginSuccess++;
						ld.success(row);
						break;
					case "failure":
						loginFailure++;
						ld.fail(row);
						break;
					default:
						break;
					}
					break;
				
				case "change":
					switch (event) {
					case "success":
						break;
					case "failure":
						break;
					default:
						break;
					}
					break;
					
				default:
					break;
				}
			}
			
			// enter counts into the loginCountResults map
			loginCountResults.put(user, new Integer[] { loginSuccess + loginFailure, loginSuccess, loginFailure });
			
			// enter durations into the loginDurationResults list
			loginDurationResults.addAll(ld.getLogs());
		}
		
		// create sorted loginCountResults key set for printing out in order
		ArrayList<String> sortedLoginCountResults = new ArrayList<>(loginCountResults.keySet());
		Collections.sort(sortedLoginCountResults);
		
		// output results of loginResults
		System.out.println("user,tries,success,failure");
		for (String user: sortedLoginCountResults) {
			Integer[] values = loginCountResults.get(user);
			StringBuilder sb = new StringBuilder();
			
			for (Integer i : values) {
				sb.append(i + ",");
			}
			
			sb.deleteCharAt(sb.length() - 1);
			System.out.println(user + "," + sb);
		} 
		
		System.out.println();
		
		// display results of duration
		System.out.println("user,result,time");
		for (Object[] row : loginDurationResults) {
			StringBuilder sb = new StringBuilder();
			
			for (Object element : row) {
				sb.append(element + ",");
			}
			
			sb.deleteCharAt(sb.length() - 1);
			System.out.println(sb);
		}
		
		System.out.println();
	}

	/**
	 * Sort the data by the date field in ascending order
	 */
	private void sortDataByDate() {
		Collections.sort(data, new Comparator<Object[]>() {
			public int compare(Object[] t1, Object[] t2) {
				return ((Date) t1[DATE]).compareTo((Date) t2[DATE]);
			};
		});
	}

	/**
	 * Return a list of all logs entries created by the user specified by
	 * parameter <code>user</code>
	 * 
	 * @param user
	 * @return
	 */
	private List<Object[]> getLogsByUser(String user) {
		ArrayList<Object[]> logs = new ArrayList<>();
		
		for (Object[] row : data) {
			if (user.equals(row[USER])) {
				logs.add(row);
			}
		}
		
		return logs;
	}

	private void parseCSVFile(String filename) throws IOException,
			ParseException {
		// read the csv file into a list of lines
		List<String> lines = FileUtils.readLines(new File(filename));
		
		// parse each line of the csv
		for (String l : lines) {
			
			// skip the first line since it defines the names of the csv columns
			if (l.startsWith("\"time\"")) {
				continue;
			}
			
			// split on ","
			String[] s = l.split("\",\"");
			
			// parse date while removing leading " (double quote)
			Date date = DATE_FORMAT.parse(s[DATE].substring(1));
			String site = s[SITE];
			String user = s[USER];
			String scheme = s[SCHEME];
			String mode = s[MODE];
			String event = s[EVENT];
			
			// parse data while removing trailing " (double quote)
			// data field may also be empty
			String data = null;
			if (s[DATA].length() > 1) {
				data = s[DATA].substring(0, s[DATA].length() - 2);
			}
			
			// create an array of the row
			Object[] values = new Object[] { date, site, user, scheme, mode, event, data };
			
			// add the row to the list of rows
			this.data.add(values);
			
			// add user to users set
			users.add(user);
		}
		
	}
	
	/**
	 * Parse the given CSV file as a command line argument.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		for (String filename : args) {
			System.out.println(filename + " ------------------------------------");
			new DataParser(filename);
		}
	}
}
