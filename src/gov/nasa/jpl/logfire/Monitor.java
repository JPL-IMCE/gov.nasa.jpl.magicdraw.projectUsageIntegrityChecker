/*
 *
 * License Terms
 *
 * Copyright (c) 2013-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nasa.jpl.logfire;

import java.lang.String;
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