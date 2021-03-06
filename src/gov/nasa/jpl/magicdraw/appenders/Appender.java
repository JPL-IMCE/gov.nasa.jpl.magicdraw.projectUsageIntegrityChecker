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
package gov.nasa.jpl.magicdraw.appenders;

import gov.nasa.jpl.logfire.LogSessionWrapper;
import gov.nasa.jpl.logfire.SessionCounter;
import gov.nasa.jpl.magicdraw.log.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.String;
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
	public static final String PROJECT_LOAD_DONE_MARKER = "ProjectLoadService DONE";

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
				log.fatal(String.format("JPL Project Usage Integrity Appender %s: Cannot create the logTraceContracts directory at: %s", name, logTraceContractsDir), e);
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
		
		log.info(String.format("JPL Project Usage Integrity Appender %s: Cleanup - Begin deleting %d old logTraceContract files in: %s", name, oldLogFiles.length, logTraceContractsDir));
		
		for (File oldLogFile : oldLogFiles) {
			try {
				log.info(String.format("JPL Project Usage Integrity Appender %s: Cleanup - Deleting old logTraceContract file: %s", name, oldLogFile.getName()));
				oldLogFile.delete();
			} catch (SecurityException e) {
				log.error(String.format("JPL Project Usage Integrity Appender %s: Cleanup - Cannot delete old logTraceContracts file: %s", name, oldLogFile), e);
			}
		}
		
		log.info(String.format("JPL Project Usage Integrity Appender %s: Cleanup - Finished deleting %d old logTraceContract files in: %s", name, oldLogFiles.length, logTraceContractsDir));
		
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
				
				Logger log = MDLog.getGeneralLog();
				String message = String.format("JPL Project Usage Integrity Appender (PROJECT_LOAD_START_MARKER): %s", e.getLocalizedMessage());
				log.error(message, e);
				Log.log(message);
			}
		} else if (eventMessage.contains(PROJECT_LOAD_DONE_MARKER)){
			if (!logSessions.isEmpty()){
				LogSessionWrapper lastWrapper = logSessions.get(logSessions.size()-1);
				FileAppender sessionFileAppender = fileAppenders.get(lastWrapper.sessionID);

				fileAppenders.remove(lastWrapper.sessionID);
				sessionFileAppender.finalize();
				activeSessions.remove(lastWrapper.sessionID);
				lastWrapper.end(true);
				logSessions.remove(lastWrapper);
			}
			
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
					Logger log = MDLog.getGeneralLog();
					String message = String.format("JPL Project Usage Integrity Appender (BEGIN_LOG_SESSION_MARKER) %s", e.getLocalizedMessage());
					log.error(message, e);
					Log.log(message);
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
				Log.log("Active session file appender missing: " + session);
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