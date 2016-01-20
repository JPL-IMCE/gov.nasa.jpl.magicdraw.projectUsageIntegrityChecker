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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands;

import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.logfire.SessionCounter;
import gov.nasa.jpl.magicdraw.log.Log;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.BufferedImageFile;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.ProjectClassification;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ui.ZoomablePanningImagePanel;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.Runnable;
import java.lang.String;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.imageio.IIOException;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.ui.Dialog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ShowProjectUsageGraphCommand implements Runnable {

	protected Project project;
	protected boolean anonymousVertexLabels;
	protected boolean includeLocalEdges;
	protected Set<String> excludedProjectNames;

	/**
	 * @param project
	 * 
	 * Constructor for analyzing a project whose usage topology should be consistent and acyclic
	 * (except for known MD's dirty laundry of local profiles/libraries)
	 * @param event 
	 */
	public ShowProjectUsageGraphCommand(@Nonnull Project project, ActionEvent event) {
		this(project, !ProjectUsageIntegrityPlugin.getInstance().showLabelsOnGraph(), true, SSCAEProjectUsageGraph.MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES);
	}

	public ShowProjectUsageGraphCommand(@Nonnull Project project,
			boolean anonymousVertexLabels,
			boolean includeLocalEdges,
			Set<String> excludedProjectNames) {
		super();
		this.project = project;
		this.anonymousVertexLabels = anonymousVertexLabels;
		this.includeLocalEdges = includeLocalEdges;
		this.excludedProjectNames = excludedProjectNames;
	}

	protected SSCAEProjectUsageGraph projectUsageGraph;

	@Override
	public void run() {

		new RunnableSessionWrapper("ShowProjectUsageGraphCommand") {
			@Override
			public void run() {

				final Logger pluginLog = MDLog.getPluginsLog();
				final ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();
				final String pluginName = plugin.getPluginName();
				final GUILog log = Application.getInstance().getGUILog();
				
				final ProjectUsageIntegrityHelper helper = plugin.getSSCAEProjectUsageIntegrityProfileForProject(project);

				ComputeProjectUsageGraphCommand _c;
				if (null == helper) {
					_c = new ComputeProjectUsageGraphCommand(project, anonymousVertexLabels, includeLocalEdges, excludedProjectNames, false);
					_c.run();
				} else {
					_c = helper.runSSCAEValidationAndShowResultsIfCheckerEnabled(anonymousVertexLabels, includeLocalEdges, excludedProjectNames, false);
				}
				final @Nonnull ComputeProjectUsageGraphCommand c = _c;

				projectUsageGraph = c.projectUsageGraph;

				final boolean ok = isProjectUsageTopologyValid();

				if (ok) {
					pluginLog.info(
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
					
					log.openMessageWindow();
					projectUsageGraph.showProblems();
					
					if (projectUsageGraph.projectClassification == ProjectClassification.INVALID) {
						Log.log(projectUsageGraph.getProjectUsageGraphDiagnostic());
						Log.log(projectUsageGraph.getProjectUsageGraphMessages());
						Log.log(projectUsageGraph.projectUsageInfo);
						return;
					}
				}

				boolean showTeamworkOnly = plugin.toggleTeamworkOrAllGraphAction.getState();
				
				if (plugin.isShowAdvancedInformationProperty()) {
					pluginLog.info(projectUsageGraph.getProjectUsageGraphSerialization());
				}
				Log.log(projectUsageGraph.getProjectUsageGraphDiagnostic());
				
				File dotFile = null;
				try {
					if (showTeamworkOnly)
						dotFile = projectUsageGraph.convertTeamworkUsageDOTgraph();
					else
						dotFile = projectUsageGraph.convertAllUsageDOTgraph();
				} catch (IIOException e) {
					pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
				} catch (IOException e) {
					pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
				} 

				if (null == dotFile) {
					pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand -- no DOT file!", pluginName));
					return;
				}
				
				if (plugin.isDotDefault()){

					// 1st, try the DOT command converter; if that works, then display the image.
					if (showWithDot(dotFile, showTeamworkOnly, pluginLog, pluginName))
						return;

					// 2nd, try opening the DOT file with graphviz
					if (showWithGV(dotFile, showTeamworkOnly, pluginLog, pluginName))
						return;

				} else {
					// 1st, try opening the DOT file with graphviz
					if (showWithGV(dotFile, showTeamworkOnly, pluginLog, pluginName))
						return;

					// 2nd, try the DOT command converter; if that works, then display the image.
					if (showWithDot(dotFile, showTeamworkOnly, pluginLog, pluginName))
						return;	
				}
				// if all else fails, just show the graph serialization (not in DOT format though)

				JTextArea textArea = new JTextArea(projectUsageGraph.getProjectUsageGraphDiagnostic());
				textArea.setColumns(100);
				textArea.setRows(60);
				textArea.setLineWrap(true);
				textArea.setEditable(false);
				JScrollPane scroll = new JScrollPane (textArea);
				scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

				Dialog dialog = new Dialog();
				dialog.setTitle(String.format("%s ProjectUsage multigraph for '%s'", 
						(showTeamworkOnly ? "Teamwork-Only" : "Local and Teamwork"),
						project.getName()));
				dialog.add(scroll);
				dialog.pack();
				dialog.setVisible(true);
			}
		};
	}

	public boolean isProjectUsageTopologyValid() { 
		if (null == projectUsageGraph)
			throw new IllegalArgumentException(String.format(
					"%s - ShowProjectUsageGraphCommand - no ProjectUsage graph",
					ProjectUsageIntegrityPlugin.getInstance().getPluginName()));

		return projectUsageGraph.isProjectUsageTopologyValid(); }

	public String getProjectUsageGraphSerialization() { 
		if (null == projectUsageGraph)
			throw new IllegalArgumentException(String.format(
					"%s - ShowProjectUsageGraphCommand - no ProjectUsage graph",
					ProjectUsageIntegrityPlugin.getInstance().getPluginName()));

		return projectUsageGraph.getProjectUsageGraphSerialization();
	}

	public String getProjectUsageGraphDiagnostic() {
		if (null == projectUsageGraph)
			throw new IllegalArgumentException(String.format(
					"%s - ShowProjectUsageGraphCommand - no ProjectUsage graph",
					ProjectUsageIntegrityPlugin.getInstance().getPluginName()));

		return projectUsageGraph.getProjectUsageGraphDiagnostic();
	}

	public boolean showWithDot(File dotFile, boolean showTeamworkOnly, Logger pluginLog, String pluginName){
		try {
			final BufferedImageFile imageFile = projectUsageGraph.convertDOTFile(dotFile, SSCAEProjectUsageGraph.DOTImageFormat.png);
			if (null != imageFile) {
				Dialog dialog = new Dialog();
				dialog.setTitle(String.format("%s ProjectUsage multigraph for '%s'", 
						(showTeamworkOnly ? "Teamwork-Only" : "Local and Teamwork"),
						project.getName()));
				dialog.add(new ZoomablePanningImagePanel(imageFile.image));
				dialog.pack();
				dialog.setVisible(true);
				return true;
			}
		} catch (IIOException e) {
			pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
		} catch (IOException e) {
			pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
		} catch (InterruptedException e) {
			pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
		} 
		return false;
	}

	public boolean showWithGV(File dotFile, boolean showTeamworkOnly, Logger pluginLog, String pluginName){
		boolean graphvizOK = false;
		try {
			graphvizOK = projectUsageGraph.openDOTFileWithGraphViz(dotFile);
		} catch (IIOException e) {
			pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
		} catch (IOException e) {
			pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
		} catch (InterruptedException e) {
			pluginLog.error(String.format("%s - ShowProjectUsageGraphCommand", pluginName), e);
		}
		if (graphvizOK)
			return true;
		return false;
	}
}