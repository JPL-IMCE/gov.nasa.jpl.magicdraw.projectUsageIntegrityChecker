package gov.nasa.jpl.logfire;

import java.util.*;

/**
 * A report is the result of a single verification of a session. It contains all
 * the violations that have been found.
 */

public class SessionReport {
	private String sessionLabel;
	private List<Violation> violations;
	private int numberOfLines;

	SessionReport(String sessionLabel, List<Violation> violations, int numberOfLines) {
		this.sessionLabel = sessionLabel;
		this.violations = violations;
		this.numberOfLines = numberOfLines;
	}

	/**
	 * Returns the label of the session verified.
	 * 
	 * @return the session label.
	 */
	public String getSessionLabel() {
		return sessionLabel;
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
	 * Returns the number of violations found.
	 * 
	 * @return the number of violations found.
	 */
	public int numberOfViolations() {
		return violations.size();
	}

	/**
	 * Returns the number of lines processed.
	 * 
	 * @return number of lines processed.
	 */
	public int numberOfLines() {
		return numberOfLines;
	}
	
	private String banner() {
		return "LOGFIRE REPORT\n\n";
	}
	
	@Override
	public String toString() {
		String string = banner();
		string = string + "session '" + sessionLabel + "' has " + numberOfLines + " lines\n\n";
		if (violations.isEmpty()) {
			string = string + "There are no errors in this session!";
		} else {
			string = string + "There are " + numberOfViolations() + " errors!\n";
			for (Violation result : violations) {
				string = string + "\n----------\n";
				string = string + result;
			}
			string = string + "\n----------\n";
		}
		return string;
	}
}
