/**
 * Copyright 2013, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged. 
 * Any commercial use must be negotiated with the Office of 
 * Technology Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. 
 * By accepting this software, the user agrees to comply with all applicable U.S. export laws 
 * and regulations. User has the responsibility to obtain export licenses,
 * or other export authority as may be required before exporting such information 
 * to foreign countries or providing access to foreign persons.
 *
 * Inquiries about this notice should be addressed to:
 *
 * JPL Software Release Authority
 * Phone: +1-818-393-3421
 * mailto:SoftwareRelease@jpl.nasa.gov
 */
package gov.nasa.jpl.logfire;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class AbstractRunnableSessionWrapper {

	public static final String BEGIN_LOG_SESSION_MARKER = "gov.nasa.jpl.logfire.beginSession(%s) : %s";
	public static final String OK_END_LOG_SESSION_MARKER = "gov.nasa.jpl.logfire.endSession(%s) : [status=OK]";
	public static final String ERROR_END_LOG_SESSION_MARKER = "gov.nasa.jpl.logfire.endSession(%s) : [status=ERROR]";

	public final String sessionID;
	
	protected int subSessionCounter = 0;
	
	public String nextSubSessionID() { return String.format("%s_%s", sessionID, SessionCounter.formatCounterValue(++subSessionCounter)); }
	
	protected final Logger log = MDLog.getPluginsLog();
	protected final Level level = log.getEffectiveLevel();
	
	protected volatile boolean failed = false;

	public void setFailed() { failed = true; }

	public AbstractRunnableSessionWrapper() {
		this.sessionID = SessionCounter.nextSession(this);
	}
}
