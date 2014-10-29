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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.ci.persistence.ProjectListener;
import com.nomagic.ci.persistence.local.util.ProjectUtil;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.core.project.RemoteProjectDescriptor;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkPrimaryProject;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.jmi.reflect.AbstractRepository;
import com.nomagic.uml2.transaction.TransactionManager;
import com.nomagic.utils.Utilities;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ProjectUsageEventListenerAdapter extends ProjectEventListenerAdapter implements PropertyChangeListener {

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
		if (!project.isRemote()) {
			Utilities.invokeOnDispatcherOrLater(new Runnable() {
				public void run() {
					plugin.toggleTeamworkOrAllGraphAction.setState(false);
				}
			});
		}
				
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
		
		project.addPropertyChangeListener(this);

		IPrimaryProject primary = project.getPrimaryProject();
		for (IAttachedProject iAttachedProject : primary.getProjects()) {
			iAttachedProject.getProjectListeners().add(helper);
		}

		project.getRepository().getTransactionManager().addTransactionCommitListener(helper.collaborationIntegrityInvariantTransactionMonitor);
	}
	
	protected void handleProjectClosed(final Project project) {
		new RunnableSessionWrapper(String.format("ProjectClose('%s')", project.getName())) {

			@Override
			public void run() {
				project.removePropertyChangeListener(ProjectUsageEventListenerAdapter.this);
				ProjectUsageIntegrityHelper helper = mProject2Profile.remove(project);
				if (helper != null) {
					AbstractRepository repo = project.getRepository();
					if (repo != null) {
						TransactionManager tm = repo.getTransactionManager();
						if (tm != null) {
							tm.removeTransactionCommitListener(helper.collaborationIntegrityInvariantTransactionMonitor);
						}
					}
					helper.dispose();
				}
			}
		};
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent ev) {
		String name = ev.getPropertyName();
		Object source = ev.getSource();
		if ("project activated".equals(name) && source instanceof Project) {
			/**
			 * @see https://support.nomagic.com/browse/MDUMLCS-13361
			 * @see https://jira1.jpl.nasa.gov:8443/browse/SSCAES-995
			 *
			 * A workaround to Donatas' suggestion, which does not work.
			 * This workaround is not optimal -- there are lots of propertyChange() notifications!
			 * Without a working POST_UPDATE notification, this is the best we can do for now.
			 */
			Project project = (Project) source;
			ProjectUsageIntegrityHelper helper = getSSCAEProjectUsageIntegrityProfileForProject(project);
			helper.flushAttachedProjectInfoCache();
			final @Nonnull Logger logger = MDLog.getPluginsLog();
			logger.info(String.format("*** ProjectUsageIntegrity.propertyChange(%s)", name));
		
		}
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
