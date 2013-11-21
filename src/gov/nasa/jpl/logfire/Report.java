package gov.nasa.jpl.logfire;

import java.util.*;

/**
 * A report is the result of a single verification of a session. It contains all
 * the violations that have been found.
 */

public class Report {
	private int sessionNumber;
	private List<Violation> violations;
	private int numberOfLinesBefore;
	private int numberOfLines;

	Report(int sessionNumber, List<Violation> violations,
			int numberOfLinesBefore, int numberOfLines) {
		this.sessionNumber = sessionNumber;
		this.violations = violations;
		this.numberOfLinesBefore = numberOfLinesBefore;
		this.numberOfLines = numberOfLines;
	}

	/**
	 * Returns the number of the session verified.
	 * 
	 * @return the session number.
	 */
	public int getSessionNumber() {
		return sessionNumber;
	}

	/**
	 * Returns the violations found.
	 * 
	 * @return the list of violations found.
	 */
	public List<Violation> getViolations() {
		return violations;
	}

	/**
	 * Returns the number of lines skipped before the session is found.
	 * 
	 * @return number of lines skipped.
	 */
	public int numberOfLinesBeforeSession() {
		return numberOfLinesBefore;
	}

	/**
	 * Returns the number of lines processed.
	 * 
	 * @return number of lines processed.
	 */
	public int numberOfLines() {
		return numberOfLines;
	}

	/**
	 * Returns the number of violations found.
	 * 
	 * @return the number of violations found.
	 */
	public int numberOfViolations() {
		return violations.size();
	}

	private String banner() {
		return "LOGFIRE REPORT\n\n";
	}
	
	@Override
	public String toString() {
		String string = banner();
		string = string + numberOfLinesBefore
				+ " lines skipped before session is found.\n";
		string = string + numberOfLines + " processed in total.\n";
		string = string + "session is " + (numberOfLines - numberOfLinesBefore)
				+ " lines long.\n\n";
		if (violations.isEmpty()) {
			string = string + "There are no errors in this session!";
		} else {
			string = string + "There are " + numberOfViolations()
					+ " errors!\n";
			for (Violation result : violations) {
				string = string + "\n----------\n";
				string = string + result;
			}
			string = string + "\n----------\n";
		}
		return string;
	}
}
