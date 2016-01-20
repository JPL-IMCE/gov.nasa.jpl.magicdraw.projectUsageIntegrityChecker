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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions;
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

import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.magicdraw.log.Log;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands.ComputeProjectUsageGraphCommand;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.icons.CheckingIcon;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.icons.InvalidIcon;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.icons.ValidIcon;

import java.awt.event.ActionEvent;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.annotation.Nonnull;
import javax.swing.Icon;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.utils.Utilities;



/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class CheckCurrentProjectUsageStatusAction extends NMAction {

	private static final long serialVersionUID = 2815932833686796182L;

	public static final Icon VALID_ICON = new ValidIcon();
	public static final Icon INVALID_ICON = new InvalidIcon();
	public static final Icon CHECKING_ICON = new CheckingIcon();


	public static final String VALID_DESCRIPTION = "JPL SSCAE PUIC Reports Valid Graph";
	public static final String INVALID_DESCRIPTION = "JPL SSCAE PUIC Reports Invalid Graph";
	public static final String CHECKING_DESCRIPTION = "JPL SSCAE PUIC Running - DO NOT CLICK";

	protected Long previousStartTime = 0L;
	protected Long previousFinishTime = 0L;
	protected boolean isChecking = false;
	protected boolean resultOfPreviousCheckIsTopologyValid = false;

	Thread thread;

	public CheckCurrentProjectUsageStatusAction() {
		super("PROJECT_USAGE_INTEGRITY_CHECK_CURRENT_PROJECT_STATUS", "STATUS", 0);

		setDescription("Check the status for the current active project");
		previousStartTime = 0L;
		previousFinishTime = 0L;
		isChecking = false;
		thread = null;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (!isChecking){
			new RunnableSessionWrapper("CheckCurrentProjectUsageGraphAction()") {

				@Override
				public void run() {
					Project project = Application.getInstance().getProject();

					if (null == project)
						return;

					computeProjectUsageGraph(project);
				}
			};
		}
	}

	protected void computeProjectUsageGraph(Project p) {
		ProjectUsageIntegrityHelper helper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(p);
		if (null != helper) {
			helper.runSSCAEValidationAndShowResultsIfCheckerEnabled(true);
			refresh();
		}
	}

	public void refresh(boolean newResult) {
		if (isChecking)
			return;
		resultOfPreviousCheckIsTopologyValid = newResult;
		refresh();
	}
	
	public void refresh() {
		if (isChecking)
			return;
		
		Utilities.invokeOnDispatcherOrLater(new Runnable() {
			public void run() {
				setDescription((resultOfPreviousCheckIsTopologyValid ? VALID_DESCRIPTION : INVALID_DESCRIPTION));
				setSmallIcon((resultOfPreviousCheckIsTopologyValid ? VALID_ICON : INVALID_ICON));
			}
		});
	}
	
	protected void computeProjectStatusThreaded(final Project p) {
		setDescription(CHECKING_DESCRIPTION);
		setSmallIcon(CHECKING_ICON);
		isChecking = true;
					
		thread = new Thread(new Runnable() {
			public void run() {
				try {
					ComputeProjectUsageGraphCommand c = new ComputeProjectUsageGraphCommand(p, false);
					c.run();
					resultOfPreviousCheckIsTopologyValid = c.isProjectUsageTopologyValid();
				} finally {
					previousFinishTime = System.currentTimeMillis();
					isChecking = false;
				}
				refresh(resultOfPreviousCheckIsTopologyValid);
			}
		});		

		thread.setUncaughtExceptionHandler(new PUIUncaughtExceptionHandler());
		thread.start();
	}

	class PUIUncaughtExceptionHandler implements UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable throwable) {
			try {
				if (throwable != null) {
					throwable.fillInStackTrace();
					throwable.printStackTrace();
					MDLog.getPluginsLog().error(throwable.getMessage(), throwable);
				}
			} finally {
				resultOfPreviousCheckIsTopologyValid = false;
			}
			Application.getInstance().getGUILog().log("ERROR: JPL Project Usage Integrity Checker: uncaught exception! Notify JPL SSCAE and restart MagicDraw (see MD log)");
		}

	}

	protected static void logError(@Nonnull Exception exception) {
		MDLog.getPluginsLog().error(exception.getMessage(), exception);
	}

	/**
	 * Must be called in on the Swing event thread.
	 */
	@Override
	public void updateState(){
		Project project = Application.getInstance().getProject();

		if (project == null) {
			setEnabled(false);
			return;
		}
		
		ProjectUsageIntegrityHelper helper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(project);
		if (helper == null) {
			return;
		}
		
		boolean mustBeEnabled = 
				isEnabled() ||
				helper.hasPostEventNotifications ||
				ProjectUsageIntegrityPlugin.getInstance().projectUsageIntegrityToggleAction.getState();
			
		if (mustBeEnabled && !isChecking) {
			if (helper.hasPostEventNotifications || (System.currentTimeMillis() - previousFinishTime) > ProjectUsageIntegrityPlugin.getInstance().getRefreshRate()*1000) {
				helper.hasPostEventNotifications = false;
				previousStartTime = System.currentTimeMillis();

				if (ProjectUsageIntegrityPlugin.getInstance().isThreadedMode()){
					computeProjectStatusThreaded(project);
				} else {
					try {
						isChecking = true;
						ComputeProjectUsageGraphCommand c = new ComputeProjectUsageGraphCommand(project, false);
						c.run();
						resultOfPreviousCheckIsTopologyValid = c.isProjectUsageTopologyValid();
					} finally {
						previousFinishTime = System.currentTimeMillis();
						isChecking = false;
					}
					refresh(resultOfPreviousCheckIsTopologyValid);
				}
			}
		}
		
		setEnabled(ProjectUsageIntegrityPlugin.getInstance().projectUsageIntegrityToggleAction.getState());
	}
}