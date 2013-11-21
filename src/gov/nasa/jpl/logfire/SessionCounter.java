/**
 * Copyright 2013, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged. 
 * Any commercial use must be negotiated with the Office of 
 * Technology Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. 
 * By acepting this software, the user agrees to comply with all applicable U.S. export laws 
 * and regulations. User has the responsibility to obtain export licenses,
 * or other export authority as may be required before exprting such information 
 * to foreign countries or providing access to foreign persons.
 *
 * Inquiries about this notice should be addressed to:
 *
 * JPL Software Release Authority
 * Phone: +1-818-393-3421
 * mailto:SoftwareRelease@jpl.nasa.gov
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
