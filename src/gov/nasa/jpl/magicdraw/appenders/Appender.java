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
package gov.nasa.jpl.magicdraw.appenders;

import gov.nasa.jpl.logfire.LogSessionWrapper;
import gov.nasa.jpl.logfire.SessionCounter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Alek Kerhzner (JPL)
 */
public class Appender extends AppenderSkeleton {

	protected HashMap <String, FileAppender> fileAppenders = new HashMap<String, FileAppender>();
	protected HashMap <String, String> fileLocations = new HashMap <String, String>();
	protected ArrayList <String> activeSessions = new ArrayList <String>();
	protected ArrayList <LogSessionWrapper> logSessions = new ArrayList <LogSessionWrapper>();
	protected LogSessionWrapper currentWrapper = null;
	
	public static final String PROJECT_LOAD_START_MARKER = "ProjectLoadService START";
	public static final String PROJECT_LOAD_MARKER = "ProjectLoadService";

	public static final String BEGIN_LOG_SESSION_MARKER = "gov.nasa.jpl.logfire.beginSession";
	public static final String END_LOG_SESSION_MARKER = "gov.nasa.jpl.logfire.endSession";
	
	protected final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.ENGLISH);
	
	protected final String logTraceContractsDirectory;
	
	public String getLogTraceContractsDirectory() { return logTraceContractsDirectory; }
		
	protected String createSessionFilename(String currentSession) {		
		return getLogTraceContractsDirectory() + dateFormat.format(new Date()) + " session-" + currentSession + ".log";
	}
	
	public Appender() {
		this.logTraceContractsDirectory = ApplicationEnvironment.getDataDirectory() + "/logTraceContracts/";
		cleanup();
	}

	protected void cleanup() {
		Logger log = MDLog.getGeneralLog();
		String name = this.getClass().getName();
		
		File logTraceContractsDir = new File(getLogTraceContractsDirectory());
		if (!logTraceContractsDir.exists()) {
			try {
				logTraceContractsDir.mkdir();
			} catch (SecurityException e) {
				log.fatal(String.format("%s: Cannot create the logTraceContracts directory at: %s", name, logTraceContractsDir), e);
			}
			return;
		}
		
		Calendar lastWeek = Calendar.getInstance();
		lastWeek.add(Calendar.DAY_OF_YEAR, -7);
		final long lastWeekTime = lastWeek.getTimeInMillis();
		
		File[] oldLogFiles = logTraceContractsDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File logFile) {
				return (logFile.lastModified() < lastWeekTime);
			}
		});
		
		log.info(String.format("%s: Cleanup - Begin deleting %d old logTraceContract files in: %s", name, oldLogFiles.length, logTraceContractsDir));
		
		for (File oldLogFile : oldLogFiles) {
			try {
				log.info(String.format("%s: Cleanup - Deleting old logTraceContract file: %s", name, oldLogFile.getName()));
				oldLogFile.delete();
			} catch (SecurityException e) {
				log.error(String.format("%s: Cleanup - Cannot delete old logTraceContracts file: %s", name, oldLogFile), e);
			}
		}
		
		log.info(String.format("%s: Cleanup - Finished deleting %d old logTraceContract files in: %s", name, oldLogFiles.length, logTraceContractsDir));
		
	}
	
	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {		
		if (null == event)
			return;
		
		String eventMessage = event.getRenderedMessage();
		if (null == eventMessage)
			return;
		
		if (eventMessage.contains(PROJECT_LOAD_START_MARKER)){
			currentWrapper = new LogSessionWrapper("Project Load");
			
			if (!activeSessions.contains(currentWrapper.sessionID))
			activeSessions.add(currentWrapper.sessionID);
			
			logSessions.add(currentWrapper);
			try {
				String fileLocation = createSessionFilename(currentWrapper.sessionID);
			
				fileLocations.put(currentWrapper.sessionID, fileLocation);
				
				FileAppender sessionFileAppender = new FileAppender(new PatternLayout("%d [%t] %-5p %c - %m%n"), fileLocation);
				fileAppenders.put(currentWrapper.sessionID, sessionFileAppender);
			} catch (IOException e) {
				SessionCounter.markSessionFailed(currentWrapper);
				Application.getInstance().getGUILog().log(e.getLocalizedMessage());
			}
		} else if (eventMessage.contains(PROJECT_LOAD_MARKER)){
			LogSessionWrapper lastWrapper = logSessions.get(logSessions.size()-1);
			FileAppender sessionFileAppender = fileAppenders.get(lastWrapper.sessionID);

			fileAppenders.remove(lastWrapper.sessionID);
			sessionFileAppender.finalize();
			activeSessions.remove(lastWrapper.sessionID);
			lastWrapper.end(true);
			logSessions.remove(lastWrapper);
			
		} else if (eventMessage.contains(BEGIN_LOG_SESSION_MARKER)) {
			int open = eventMessage.indexOf("(");
			int close = eventMessage.indexOf(")");
			
			String session = eventMessage.substring(open+1, close);
			
			if (!activeSessions.contains(session)){
				activeSessions.add(session);
				try {
					String fileLocation = createSessionFilename(session);
					fileLocations.put(session, fileLocation);
					
					FileAppender sessionFileAppender = new FileAppender(new PatternLayout("%d [%t] %-5p %c - %m%n"), fileLocation);
					fileAppenders.put(session, sessionFileAppender);
				} catch (IOException e) {
					Application.getInstance().getGUILog().log(e.getLocalizedMessage());
				}				
			}
			
		} else if (eventMessage.contains(END_LOG_SESSION_MARKER)) {
			int open = eventMessage.indexOf("(");
			int close = eventMessage.indexOf(")");
			
			String session = eventMessage.substring(open+1, close);
			
			if (activeSessions.contains(session) && fileAppenders.containsKey(session)){
				FileAppender sessionFileAppender = fileAppenders.get(session);
				
				sessionFileAppender.append(event);
				fileAppenders.remove(session);
				sessionFileAppender.finalize();
				activeSessions.remove(session);
			}				
		}
		
		for (String session : activeSessions){
			if (fileAppenders.containsKey(session)) {
				FileAppender appender = fileAppenders.get(session);
				appender.append(event);
			} else {
				Application.getInstance().getGUILog().log("Active session file appender missing: " + session);
			}
		}
		
	}
	
	public HashMap <String, String> getFileLocations(){
		return fileLocations;
	}
	
	public String getFileLocation(String session){
		return fileLocations.get(session);
	}
	
}
