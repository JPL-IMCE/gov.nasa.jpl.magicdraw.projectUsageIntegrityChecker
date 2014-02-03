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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands;

import gov.nasa.jpl.logfire.SessionCounter;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.BufferedImageFile;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph.DOTImageFormat;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.imageio.IIOException;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.utils.Utilities;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ComputeProjectUsageGraphCommand implements Runnable {

	protected Project project;
	protected boolean anonymousVertexLabels;
	protected boolean includeLocalEdges;
	protected Set<String> excludedProjectNames;
	protected boolean showProjectUsageDiagnosticModalDialog;

	/**
	 * @param project
	 * 
	 * Constructor for analyzing a project whose usage topology should be consistent and acyclic
	 * (except for known MD's dirty laundry of local profiles/libraries)
	 */

	public ComputeProjectUsageGraphCommand(@Nonnull Project project, boolean showProjectUsageDiagnosticModalDialog) {
		this(project, !ProjectUsageIntegrityPlugin.getInstance().showLabelsOnGraph(), true, SSCAEProjectUsageGraph.MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES, showProjectUsageDiagnosticModalDialog);
	}

	public ComputeProjectUsageGraphCommand(@Nonnull Project project,
			boolean anonymousVertexLabels,
			boolean includeLocalEdges,
			Set<String> excludedProjectNames, boolean createModelMessages) {
		super();
		this.project = project;
		this.anonymousVertexLabels = anonymousVertexLabels;
		this.includeLocalEdges = includeLocalEdges;
		this.excludedProjectNames = excludedProjectNames;
		this.showProjectUsageDiagnosticModalDialog = createModelMessages;
	} 

	public SSCAEProjectUsageGraph projectUsageGraph;

	@Override
	public void run() {
		
		if (!ProjectUsageIntegrityPlugin.getInstance().isProjectUsageIntegrityCheckerEnabled()) {
			return;
		}
		
		try {
			projectUsageGraph = new SSCAEProjectUsageGraph(project, anonymousVertexLabels, includeLocalEdges, excludedProjectNames);
		} catch (RemoteException e) {
			MDLog.getPluginsLog().error(String.format("%s", ProjectUsageIntegrityPlugin.getInstance().getPluginName()), e);
			return;
		}

		final boolean ok = isProjectUsageTopologyValid();
		boolean printLog = true;

		@Nonnull ProjectUsageIntegrityHelper puiHelper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(project);
		if (puiHelper.latestProjectUsageGraph != null && puiHelper.latestProjectUsageGraph.isProjectUsageTopologyValid() && projectUsageGraph.isProjectUsageTopologyValid()) {
			if (projectUsageGraph.isIsomorphicWith(puiHelper.latestProjectUsageGraph)){
				printLog = false;
			}
		}
		puiHelper.latestProjectUsageGraph = projectUsageGraph;
		
		if (ok) {
			if (printLog)
				MDLog.getPluginsLog().info(
						String.format("%s - OK - ProjectUsage graph is valid\n%s",
								ProjectUsageIntegrityPlugin.getInstance().getPluginName(),
								getProjectUsageGraphDiagnostic()));
		} else {
			if (SessionCounter.hasCurrentSession())
				SessionCounter.markCurrentSessionFailed();

			MDLog.getPluginsLog().error(
					String.format("%s - Error - ProjectUsage graph is invalid\n%s",
							ProjectUsageIntegrityPlugin.getInstance().getPluginName(),
							getProjectUsageGraphDiagnostic()));
		}

		if (this.showProjectUsageDiagnosticModalDialog) {
			Utilities.invokeOnDispatcherOrLater(new Runnable() {
				public void run() {
					Application.getInstance().getGUILog().openMessageWindow();
					projectUsageGraph.getDigest().showProblems();
					ProjectUsageIntegrityPlugin.getInstance().checkCurrentProjectStatusAction.refresh(ok);
					if (ok)
						Application.getInstance().getGUILog().showMessage(getProjectUsageGraphDiagnostic());
					else
						Application.getInstance().getGUILog().showError(getProjectUsageGraphDiagnostic());
				}
			});
		}
	}

	public boolean isProjectUsageTopologyValid() {  
			if (projectUsageGraph == null){
				run();
			}
			
			if (!ProjectUsageIntegrityPlugin.getInstance().isProjectUsageIntegrityCheckerEnabled()) {
				return false;
			}
			
			return projectUsageGraph.isProjectUsageTopologyValid(); 
	}
	
	public boolean isLocalTemplate() { return projectUsageGraph.isLocalTemplate(); }

	public String getProjectUsageGraphMessages() { return projectUsageGraph.getProjectUsageGraphMessages(); }

	public String getProjectUsageGraphSerialization() { return projectUsageGraph.getProjectUsageGraphSerialization(); }

	public String getProjectUsageGraphDiagnostic() { 
		return String.format("Project classification: %s\n%s",
				projectUsageGraph.getProjectClassificationLabel(), projectUsageGraph.getProjectUsageGraphDiagnostic());
	}

	public File convertAllUsageDOTgraph() throws IOException {
		return projectUsageGraph.convertAllUsageDOTgraph();
	}

	public File convertTeamworkUsageDOTgraph() throws IOException {
		return projectUsageGraph.convertTeamworkUsageDOTgraph();
	}

	public BufferedImageFile convertDOTFile(@Nonnull File pugDOT, @Nonnull DOTImageFormat dotImageFormat) throws IIOException, IOException, InterruptedException {
		return projectUsageGraph.convertDOTFile(pugDOT, dotImageFormat);
	}
	
	public boolean openDOTFileWithGraphViz(@Nonnull File pugDOT) throws IIOException, IOException, InterruptedException {
		return projectUsageGraph.openDOTFileWithGraphViz(pugDOT);
	}
}
