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

import java.util.*;
import java.lang.String;

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