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

import java.util.Stack;

import javax.annotation.Nonnull;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public final class SessionCounter {

	private static int counter = 0;
	
	protected static final Stack<AbstractRunnableSessionWrapper> wrapperStack = new Stack<AbstractRunnableSessionWrapper>();
	
	private SessionCounter() {}
	
	public static String formatCounterValue(int c) {
		return String.format(((c < 10) ? "0%d" : "%d"), c);
	}
	
	public static synchronized String nextSession(final @Nonnull AbstractRunnableSessionWrapper wrapper) {
		if (wrapperStack.isEmpty()) {
			wrapperStack.push(wrapper);
			return formatCounterValue(++counter);
		}
		
		String subSessionID = wrapperStack.peek().nextSubSessionID();
		wrapperStack.push(wrapper);
		return subSessionID;
	}
	
	public static synchronized boolean hasCurrentSession() { 
		return (!wrapperStack.isEmpty());
	}
			
	public static synchronized void markCurrentSessionFailed() { 
		if (wrapperStack.isEmpty())
			throw new IllegalArgumentException("markCurrentSessionFailed -- wrapper stack is empty!");
		
		markSessionFailed(wrapperStack.peek());
	}
	
	public static synchronized void markSessionFailed(final @Nonnull AbstractRunnableSessionWrapper wrapper) { 
		wrapper.setFailed();
	}
	
	public static synchronized void finishedSession(final @Nonnull AbstractRunnableSessionWrapper wrapper) {
		if (wrapperStack.isEmpty())
			throw new IllegalArgumentException(String.format(
					"finishedSession(%s) but the wrapper stack is empty!", 
					wrapper.sessionID));
		
		if (! wrapper.sessionID.equals(wrapperStack.peek().sessionID)){
			if (!wrapperStack.peek().sessionID.startsWith(wrapper.sessionID + "_")){
			throw new IllegalArgumentException(String.format(
					"finishedSession(%s) mismatch with the top of the wrapper stack (%ss)", 
					wrapper.sessionID, wrapperStack.peek().sessionID));
			} else {
				while (wrapperStack.peek().sessionID.startsWith(wrapper.sessionID + "_")){
					wrapperStack.pop();
				}
			}
			
		}

		wrapperStack.pop();
	}
}