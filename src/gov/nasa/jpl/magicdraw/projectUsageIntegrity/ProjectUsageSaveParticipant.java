/**
 * Copyright 2013, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged. 
 * Any commercial use must be negotiated with the Office of 
 * Technology Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. 
 * By acepting this software, the user agrees to comply with all applicable U.S. export laws 
 * and regulations. User has the responsibility to odescriptorbtain export licenses,
 * or other export authority as may be required before exprting such information 
 * to foreign countries or providing access to foreign persons.
 *
 * Inquiries about this notice should be addressed to:
 *
 * JPL Software Release Authority
 * Phone: +1-818-393-3421
 * mailto:SoftwareRelease@jpl.nasa.gov
 */
package gov.nasa.jpl.magicdraw.projectUsageIntegrity;

import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.logfire.RunnableSessionWrapperWithResult;
import gov.nasa.jpl.logfire.SessionCounter;
import gov.nasa.jpl.magicdraw.log.Log;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands.ComputeProjectUsageGraphCommand;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.OperationCanceledException;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.SaveParticipant;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.jmi.EventSupport;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ProjectUsageSaveParticipant implements SaveParticipant {

	@Override
	public boolean isReadyForSave(final Project project, final ProjectDescriptor descriptor) {

		Boolean ok = new RunnableSessionWrapperWithResult<Boolean>(String.format("isReadyForSave('%s')", project.getName())) {

			@Override
			protected Boolean run() {
				Logger logger = MDLog.getPluginsLog();
				ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();

				if (!plugin.isProjectUsageIntegrityCheckerEnabled()) 
					return Boolean.TRUE;

				ProjectUsageIntegrityHelper helper = plugin.getSSCAEProjectUsageIntegrityProfileForProject(project);
				if (null == helper)
					return Boolean.TRUE;
				
				boolean allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
				if (!allResourcesInProject)
					throw new OperationCanceledException("Some project resources have lost their project relationship! isReadyForSave=false");
									
				final ComputeProjectUsageGraphCommand c = helper.runSSCAEValidationAndShowResultsIfCheckerEnabled(false);
				if (c.isProjectUsageTopologyValid()) {
					logger.info(
							String.format("%s - isReadyForSave() = true - ProjectUsage graph is valid\ndiagnostic:\n%s",
									plugin.getPluginName(),
									c.getProjectUsageGraphDiagnostic()));

					return Boolean.TRUE;
				} else {
					SessionCounter.markCurrentSessionFailed();

					logger.error(
							String.format("%s - isReadyForSave() = false - ProjectUsage graph is invalid\ndiagnostic:\n%s",
									plugin.getPluginName(),
									c.getProjectUsageGraphDiagnostic()));

					Logger log = MDLog.getGeneralLog();
					log.error(String.format("JPL Project Usage Integrity Appender (isReadyForSave): Cannot save project '%s' because its ProjectUsage graph is invalid\n%s", 
							project.getName(),
							c.getProjectUsageGraphDiagnostic()));
					GUILog glog = Application.getInstance().getGUILog();
					glog.clearLog();
					Log.log(String.format("*** Cannot save project '%s' because its ProjectUsage graph is invalid (See MD's log) ***",
							project.getName()));
					return Boolean.FALSE;
				}
			}
		}.execute();
		if (null == ok)
			return true;
		
		return ok;
	}

	@Override
	public void doBeforeSave(final Project project, final ProjectDescriptor descriptor) {
		final ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();
		final String doBeforeSaveMessage = String.format("%s - doBeforeSave('%s')", plugin.getPluginName(), project.getName());
		new RunnableSessionWrapper(doBeforeSaveMessage) {

			@Override
			public void run() {
				final Logger logger = MDLog.getPluginsLog();
				
				final ProjectUsageIntegrityHelper helper = plugin.getSSCAEProjectUsageIntegrityProfileForProject(project);
				if (null == helper || ! helper.resolvedSSCAEProfileAndStereotypes())
					return;
				
				boolean allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
				if (!allResourcesInProject)
					throw new OperationCanceledException("Some project resources have lost their project relationship! save aborted");

				final Model m = project.getModel();
						
				if (!plugin.isProjectUsageIntegrityCheckerEnabled()) {
					clearSSCAEPropertiesBeforeSave(project, helper, m);
					return;
				}
				
				final ComputeProjectUsageGraphCommand c = helper.runSSCAEValidationAndShowResultsIfCheckerEnabled(false);
				c.run();
				
				if (!c.isProjectUsageTopologyValid()) {
					SessionCounter.markCurrentSessionFailed();
					logger.error(
							String.format("%s - isReadyForSave() = false - ProjectUsage graph is invalid\ndiagnostic:\n%s",
									plugin.getPluginName(),
									c.getProjectUsageGraphDiagnostic()));
					throw new OperationCanceledException(String.format("Commit to teamwork is canceled because the ProjectUsage graph is invalid\n%s", c.getProjectUsageGraphDiagnostic()));
				}
					
				final String newSerialization = c.getProjectUsageGraphSerialization();
				final String newDiagnostic = c.getProjectUsageGraphDiagnostic();
				
				if (!helper.latestProjectUsageGraph.isProjectUsageTopologyValid()) {
					SessionCounter.markCurrentSessionFailed();
					logger.error(String.format("%s - ERROR - ProjectUsage graph is invalid\ndiagnostic:\n%s", doBeforeSaveMessage, newDiagnostic));
					throw new OperationCanceledException(String.format("Commit to teamwork is canceled because the ProjectUsage graph is invalid\n%s", newDiagnostic));
				}

				if (ApplicationEnvironment.isDeveloper())
					logger.info(String.format("%s - OK - ProjectUsage graph is valid\ndiagnostic:\n%s", doBeforeSaveMessage, newDiagnostic));

				if (helper.hasSSCAEProjectModelStereotypeApplied(m)) {
					final int version = 1 + helper.getSSCAEProjectModelVersion(m);
					final String oldSerialization = helper.getSSCAESharedPackageGraphSerialization(m);
					if (helper.latestProjectUsageGraph.isLocalTemplate() || oldSerialization == null || !oldSerialization.equals(newSerialization)) {
						final String sessionLabel = String.format("doBeforeSave('%s') - updating SSCAE ProjectUsage info for project model {ID=%s}", project.getName(), project.getID());
						new RunnableSessionWrapper(sessionLabel) {

							@Override
							public void run() {
								final SessionManager sm = SessionManager.getInstance();
								sm.createSession(sessionLabel);
								try {
									
									helper.setSSCAEProjectModelVersion(m, version);

									boolean allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
									if (!allResourcesInProject)
										throw new OperationCanceledException("Save1: Some project resources have lost their project relationship! save aborted");

									helper.setSSCAEProjectModelGraphSerialization(m, newSerialization);
	
									allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
									if (!allResourcesInProject)
										throw new OperationCanceledException("Save2: Some project resources have lost their project relationship! save aborted");

									if (sm.isSessionCreated())
										sm.closeSession();

									allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
									if (!allResourcesInProject)
										throw new OperationCanceledException("Save3: Some project resources have lost their project relationship! save aborted");

								} catch (Exception e) {
									logger.error(sessionLabel, e);
									if (sm.isSessionCreated())
										sm.cancelSession();
								}
							}
						};
					}
				}
				
				for (final Package managedSharedPackage : helper.latestProjectUsageGraph.managedSharedPackages) {
					final int version = 1 + helper.getSSCAESharedPackageVersion(managedSharedPackage);
					final String sessionPackageLabel = String.format("doBeforeSave('%s') - updating SSCAE ProjectUsage info for shared package %s", project.getName(), managedSharedPackage.getQualifiedName());
					new RunnableSessionWrapper(sessionPackageLabel) {

						@Override
						public void run() {
							final SessionManager sm = SessionManager.getInstance();
							sm.createSession(sessionPackageLabel);
							try {

								helper.setSSCAESharedPackageVersion(managedSharedPackage, version);

								boolean allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
								if (!allResourcesInProject)
									throw new OperationCanceledException("Save4: Some project resources have lost their project relationship! save aborted");

								helper.setSSCAESharedPackageGraphSerialization(managedSharedPackage, newSerialization);

								allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
								if (!allResourcesInProject)
									throw new OperationCanceledException("Save5: Some project resources have lost their project relationship! save aborted");

								if (sm.isSessionCreated())
									sm.closeSession();

								allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
								if (!allResourcesInProject)
									throw new OperationCanceledException("Save6: Some project resources have lost their project relationship! save aborted");

							} catch (Exception e) {
								logger.error(sessionPackageLabel, e);
								if (sm.isSessionCreated())
									sm.cancelSession();
							}
						}
					};
				}
			}
		};		
	}

	protected void clearSSCAEPropertiesBeforeSave(final Project project, final ProjectUsageIntegrityHelper helper, final Model m) {
		final Logger logger = MDLog.getPluginsLog();
		final String newSerialization = "*** MD5 signature deleted because the SSCAE ProjectUsage Integrity Checker is disabled ***";

		if (helper.hasSSCAEProjectModelStereotypeApplied(m)) {
			final int version = 1 + helper.getSSCAEProjectModelVersion(m);
		
			final String sessionLabel = String.format("doBeforeSave('%s') - clearing SSCAE ProjectUsage info for project model {ID=%s}", project.getName(), project.getID());

			new RunnableSessionWrapper(sessionLabel) {

				@Override
				public void run() {
					final SessionManager sm = SessionManager.getInstance();
					sm.createSession(sessionLabel);
					try {

						helper.setSSCAEProjectModelVersion(m, version);

						boolean allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
						if (!allResourcesInProject)
							throw new OperationCanceledException("Clear1: Some project resources have lost their project relationship! save aborted");

						helper.setSSCAEProjectModelGraphSerialization(m, newSerialization);

						allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
						if (!allResourcesInProject)
							throw new OperationCanceledException("Clear2: Some project resources have lost their project relationship! save aborted");

						if (sm.isSessionCreated())
							sm.closeSession();

						allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
						if (!allResourcesInProject)
							throw new OperationCanceledException("Clear3: Some project resources have lost their project relationship! save aborted");

					} catch (Exception e) {
						logger.error(sessionLabel, e);
						if (sm.isSessionCreated())
							sm.cancelSession();
					}
				}
			};
		}
		if (helper.latestProjectUsageGraph != null){
			for (final Package managedSharedPackage : helper.latestProjectUsageGraph.managedSharedPackages) {
				final int version = 1 + helper.getSSCAESharedPackageVersion(managedSharedPackage);
				final String sessionPackageLabel = String.format("doBeforeSave('%s') - clearing SSCAE ProjectUsage info for shared package %s", project.getName(), managedSharedPackage.getQualifiedName());
				new RunnableSessionWrapper(sessionPackageLabel) {
	
					@Override
					public void run() {
						final SessionManager sm = SessionManager.getInstance();
						sm.createSession(sessionPackageLabel);
						try {
	
							helper.setSSCAESharedPackageVersion(managedSharedPackage, version);
	
							boolean allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
							if (!allResourcesInProject)
								throw new OperationCanceledException("Clear4: Some project resources have lost their project relationship! save aborted");
	
							helper.setSSCAESharedPackageGraphSerialization(managedSharedPackage, newSerialization);
	
							allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
							if (!allResourcesInProject)
								throw new OperationCanceledException("Clear5: Some project resources have lost their project relationship! save aborted");
	
							if (sm.isSessionCreated())
								sm.closeSession();
	
							allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
							if (!allResourcesInProject)
								throw new OperationCanceledException("Clear6: Some project resources have lost their project relationship! save aborted");
	
						} catch (Exception e) {
							logger.error(sessionPackageLabel, e);
							if (sm.isSessionCreated())
								sm.cancelSession();
						}
					}
				};
			}
		}
	}
	
	/**
	 * Not called for teamwork projects
	 * @see https://support.nomagic.com/browse/MDUMLCS-8884
	 */
	@Override
	public void doAfterSave(final Project project, final ProjectDescriptor descriptor) {
		final ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();
		final String doAfterSaveMessage = String.format("%s - doAfterSave('%s')", plugin.getPluginName(), project.getName());
		new RunnableSessionWrapper(doAfterSaveMessage) {

			@Override
			public void run() {
				final Logger logger = MDLog.getPluginsLog();

				final ProjectUsageIntegrityHelper helper = plugin.getSSCAEProjectUsageIntegrityProfileForProject(project);
				if (null == helper || ! helper.resolvedSSCAEProfileAndStereotypes())
					return;

				if (!helper.isEnabled())
					return;
				
				if (helper.latestProjectUsageGraph == null)
					return;
				
				boolean allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
				if (!allResourcesInProject)
					throw new OperationCanceledException("Some project resources have lost their project relationship! isReadyForSave=false");

				allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
				if (!allResourcesInProject)
					throw new OperationCanceledException("Save7: Some project resources have lost their project relationship! save aborted");

				if (!helper.latestProjectUsageGraph.isProjectUsageTopologyValid()) {
					SessionCounter.markCurrentSessionFailed();

					logger.error(
							String.format("%s - ERROR - ProjectUsage graph is invalid\n%s",
									doAfterSaveMessage,
									helper.latestProjectUsageGraph.getProjectUsageGraphDiagnostic()));

					throw new OperationCanceledException(String.format("Commit to teamwork is canceled because the ProjectUsage graph is invalid\n%s",
							helper.latestProjectUsageGraph.getProjectUsageGraphDiagnostic()));
				}

				// workaround
				// @see https://support.nomagic.com/browse/MDUMLCS-8884?focusedCommentId=50079#action_50079
				// @see https://support.nomagic.com/browse/MDUMLCS-2103
				EventSupport eventSupport = project.getRepository().getEventSupport();
				eventSupport.setEnableEventFiring(false);

				allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
				if (!allResourcesInProject)
					throw new OperationCanceledException("Save8: Some project resources have lost their project relationship! save aborted");

				eventSupport.setEnableEventFiring(true);

				allResourcesInProject = helper.checkIProjectResources(project.getPrimaryProject());
				if (!allResourcesInProject)
					throw new OperationCanceledException("Save9: Some project resources have lost their project relationship! save aborted");
			}
		};

	}
}
