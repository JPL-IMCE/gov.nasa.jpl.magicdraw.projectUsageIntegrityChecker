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
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands.ShowProjectUsageGraphCommand;

import java.awt.event.ActionEvent;

import javax.annotation.Nonnull;

import com.nomagic.actions.ActionsVisitor;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ShowCurrentProjectUsageGraphAction extends NMAction {

	private static final long serialVersionUID = 2233793696475346703L;

	public ShowCurrentProjectUsageGraphAction() {
		super("PROJECT_USAGE_INTEGRITY_SHOW_CURRENT_PROJECT_USAGE_GRAPH", "GRAPH", 0);

		setDescription("Show the ProjectUsage graph for the current active project");
	}

	@Override
	public void accept(ActionsVisitor v) {
		super.accept(v);
	}
	
	@Override
	public void actionPerformed(final ActionEvent event) {
		new RunnableSessionWrapper("ShowCurrentProjectUsageGraphAction()") {

			@Override
			public void run() {
				Project project = Application.getInstance().getProject();

				if (null == project)
					return;

				computeProjectUsageGraph(project, event);
			}
		};
	}

	protected void computeProjectUsageGraph(Project p, ActionEvent event) {
		ShowProjectUsageGraphCommand s = new ShowProjectUsageGraphCommand(p, event);
		s.run();
	}

	protected static void logError(@Nonnull Exception exception) {
		MDLog.getPluginsLog().error(exception.getMessage(), exception);
	}
	
	/**
	 * Must be called in on the Swing event thread.
	 */
	@Override
	public void updateState(){
		if (!ProjectUsageIntegrityPlugin.getInstance().isProjectUsageIntegrityCheckerEnabled()) {
			setEnabled(false);
			return;
		} else {
			setEnabled(true);
		}
		
		Project project = Application.getInstance().getProject();

		setEnabled(project != null);
	}
}
