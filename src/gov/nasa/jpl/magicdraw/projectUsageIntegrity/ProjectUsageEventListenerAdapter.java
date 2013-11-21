package gov.nasa.jpl.magicdraw.projectUsageIntegrity;
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
import gov.nasa.jpl.logfire.SessionCounter;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.core.project.RemoteProjectDescriptor;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkPrimaryProject;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ProjectUsageEventListenerAdapter extends ProjectEventListenerAdapter {

	protected final ProjectUsageIntegrityPlugin plugin;
	
	public ProjectUsageEventListenerAdapter(final ProjectUsageIntegrityPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void projectOpened(Project project) {
		handleProjectOpen(project);
	}
	
	@Override
	public void projectClosed(Project project) {
		handleProjectClosed(project);
	}
	
	@Override
	public void projectSaved(Project project, boolean savedInServer) {
		handleProjectSaved(project, savedInServer);
	}

	protected Map<Project, ProjectUsageIntegrityHelper> mProject2Profile = new HashMap<Project, ProjectUsageIntegrityHelper>();

	public ProjectUsageIntegrityHelper getSSCAEProjectUsageIntegrityProfileForProject(@Nonnull Project project) {
		if (!project.isRemote())
			plugin.toggleTeamworkOrAllGraphAction.setState(false);
				
		if (!mProject2Profile.containsKey(project)) {
			ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();

			try {
				final ProjectUsageIntegrityHelper helper = new ProjectUsageIntegrityHelper(project, plugin.projectUsageIntegrityToggleAction);
				mProject2Profile.put(project, helper);
			} finally {
				
			}
			
		}
		
		return mProject2Profile.get(project);
	}
	
	protected void handleProjectOpen(final Project project) {
		// Do not run on project open, run on UI refresh. 
		// Prevents checker from running twice on project open
		ProjectUsageIntegrityHelper helper = getSSCAEProjectUsageIntegrityProfileForProject(project);
		helper.hasPostEventNotifications = true;	
	}
	
	protected void handleProjectClosed(final Project project) {
		new RunnableSessionWrapper(String.format("ProjectClose('%s')", project.getName())) {

			@Override
			public void run() {
				ProjectUsageIntegrityHelper helper = mProject2Profile.remove(project);
				if (helper != null){
					helper.dispose();
				}
			}
		};
	}
	
	private boolean doSecondCommitAfterCleanCommit = false;
	
	protected void handleProjectSaved(final Project project, final boolean savedInServer) {
		final @Nonnull Logger logger = MDLog.getPluginsLog();
		final @Nonnull ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();
		final @Nonnull String doAfterSaveMessage = String.format("%s - doAfterSave('%s')", plugin.getPluginName(), project.getName());
		
		new RunnableSessionWrapper(doAfterSaveMessage) {

			@Override
			public void run() {
				if (!plugin.isProjectUsageIntegrityCheckerEnabled()) {
					doSecondCommitAfterCleanCommit = false;
					return;
				}
		
				final ProjectUsageIntegrityHelper helper = plugin.getSSCAEProjectUsageIntegrityProfileForProject(project);
				if (null == helper || ! helper.resolvedSSCAEProfileAndStereotypes()) {
					doSecondCommitAfterCleanCommit = false;
					return;
				}
				
				if (! project.isTeamworkServerProject()) {
					doSecondCommitAfterCleanCommit = false;
					return;
				}
				
				if (! savedInServer) {
					doSecondCommitAfterCleanCommit = false;
					return;
				}
				
				if (doSecondCommitAfterCleanCommit) {
					doSecondCommitAfterCleanCommit = false;
					logger.info(String.format("%s - 1st and 2nd phase commit done", doAfterSaveMessage));
				} else {
					String sscaeCleanTag;
					try {
						sscaeCleanTag = helper.computeTeamworkProjectSSCAECleanCommitTag();
					} catch (IllegalArgumentException e) {
						logger.warn(String.format("%s - Error during 1st phase commit while computing the SSCAE Clean Commit Tag", doAfterSaveMessage), e);
						SessionCounter.markCurrentSessionFailed();
						return;
					}
				
					final @Nonnull TeamworkPrimaryProject pp = (TeamworkPrimaryProject) project.getPrimaryProject();
					final List<String> tags = pp.getTags();
					final List<String> newTags = new ArrayList<String>();
					newTags.addAll(tags);
					newTags.add(sscaeCleanTag);
					try {
						logger.info(String.format("%s - 1st phase commit - adding SSCAE clean tag", doAfterSaveMessage));
						ProjectDescriptor descriptor = new RemoteProjectDescriptor(project);
						TeamworkUtils.setTags(descriptor, newTags, -1);
						logger.info(String.format("%s - 1st phase commit - added Teamwork tag: '%s'", doAfterSaveMessage, sscaeCleanTag));
					} catch (RemoteException e) {
						logger.warn(String.format("%s - Error during 1st phase commit while adding the SSCAE Clean Commit Tag: %s", doAfterSaveMessage, sscaeCleanTag), e);
						SessionCounter.markCurrentSessionFailed();
					}
					
					doSecondCommitAfterCleanCommit = true;
					
					boolean ok = false;
					try {
						logger.info(String.format("%s - 2/2: Committing a new version", doAfterSaveMessage));
						ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
						ProjectDescriptor descriptor = new RemoteProjectDescriptor(project);
						ok = projectsManager.saveProject(descriptor, true);
					} finally {
						if (ok) {
							logger.info(String.format("%s - 2nd phase commit successful", doAfterSaveMessage));
						} else {
							logger.error(String.format("%s - Error during 2nd phase commit", doAfterSaveMessage));
							SessionCounter.markCurrentSessionFailed();
						}
					}
				}
			}
		};	
	}

}
