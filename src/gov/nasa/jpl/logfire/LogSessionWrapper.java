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

import javax.annotation.Nonnull;

/**
 * @author Alek Kerzhner (JPL)
 */

public class LogSessionWrapper extends AbstractRunnableSessionWrapper {

	public LogSessionWrapper(@Nonnull String message){
		super();
		log.log(level, String.format(BEGIN_LOG_SESSION_MARKER, sessionID, message));
	}
	
	public void end(@Nonnull Boolean ok){
		log.log(level, String.format((ok ? OK_END_LOG_SESSION_MARKER : ERROR_END_LOG_SESSION_MARKER), sessionID));
		SessionCounter.finishedSession(this);
	}
}
