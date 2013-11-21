package gov.nasa.jpl.logfire;

/**
 * Represents a violation. A violation is identified by
 * the violating log file line number, the bad text that it contains, and 
 * the log line itself.
 */

public class Violation {
	private int lineNumber;
	private String badText;
	private String line;

	Violation(int lineNumber, String badText, String line) {
		this.lineNumber = lineNumber;
		this.badText = badText;
		this.line = line;
	}

	/**
	 * @return the lineNumber
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * @return the badText
	 */
	public String getBadText() {
		return badText;
	}

	/**
	 * @return the line
	 */
	public String getLine() {
		return line;
	}

	@Override
	public String toString() {
		return "line " + lineNumber + " : " + badText + "\n" + line;
	}
}
