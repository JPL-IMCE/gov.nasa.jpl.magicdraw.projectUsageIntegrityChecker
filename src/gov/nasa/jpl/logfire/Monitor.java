package gov.nasa.jpl.logfire;

import java.util.*;

/**
 * Offers the features needed for verifying a log file against a
 * specification. A <i>specification</i> is a set of text strings. A <i>log</i>
 * violates a specification if it contains a line that contains one of these
 * text strings.
 */

public class Monitor {
	private List<String> badTexts;
	private String logFileName;

	/**
	 * The constuctor is called with two file names, one identifying the
	 * specification and one identifying the log file.
	 * 
	 * @param specFileName
	 *            name of the specification file.
	 * @param logFileName
	 *            name of the log file.
	 */
	public Monitor(String specFileName, String logFileName) {
		badTexts = new FileReader(specFileName).readFile();
		this.logFileName = logFileName;
	}

	public SessionReport verifyWholeSession(String sessionLabel) {
		FileReader log = new FileReader(logFileName);
		List<Violation> violations = new ArrayList<Violation>();
		int lineNumber = 1;
		String line = log.readLine();
		while (line != null) {
			List<String> matches = verifyLine(line);
			for (String match : matches) {
				violations.add(new Violation(lineNumber, match, line));
			}
			line = log.readLine();
		}

		return new SessionReport(sessionLabel, violations, lineNumber);
	}
	
	/**
	 * Called to verify a session. The method returns a report describing the
	 * result of the verification.
	 * 
	 * @param sessionNumber
	 *            the number of the session that should be verified.
	 * @return the report documenting potential violations found.
	 */
	public Report verifySession(int sessionNumber) {
		FileReader log = new FileReader(logFileName);
		List<Violation> violations = new ArrayList<Violation>();
		String begin = "logfire.open(" + sessionNumber + ")";
		String end = "logfire.close(" + sessionNumber + ")";
		String line;
		int lineNumber;
		int lineNumberBefore;
		
		line = log.readLine();
		lineNumber = 1;
		
		while (line != null && !line.contains(begin)) {
			line = log.readLine();
			lineNumber++;
		}

		lineNumberBefore = lineNumber - 1;
		
		if (line != null) {
			line = log.readLine();
			lineNumber++;

			while (line != null && !line.contains(end)) {
				List<String> matches = verifyLine(line);
				for (String match : matches) {
					violations.add(new Violation(lineNumber, match, line));
				}
				line = log.readLine();
				lineNumber++;
			}
			if (line == null) {
				lineNumber--;
				System.out.println("Sessions " + sessionNumber + " not terminated correctly");
			}
		} else {
			lineNumber--;
			System.out.println("Session " + sessionNumber + " not found");
		}

		return new Report(sessionNumber, violations, lineNumberBefore, lineNumber);
	}

	private List<String> verifyLine(String line) {
		List<String> badTextsFound = new ArrayList<String>();
		for (String badText : badTexts) {
			if (line.contains(badText)) {
				badTextsFound.add(badText);
			}
		}
		return badTextsFound;
	}
}
