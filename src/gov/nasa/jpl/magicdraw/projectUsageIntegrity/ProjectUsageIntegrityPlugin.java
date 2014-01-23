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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity;

import gov.nasa.jpl.magicdraw.appenders.Appender;
import gov.nasa.jpl.magicdraw.appenders.AppendingOutputStream;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.CheckCurrentProjectUsageStatusAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ShowCurrentProjectUsageGraphAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ToggleGraphLabelAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ToggleProjectUsageIntegrityCheckerAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ToggleProjectUsageTeamworkOrAllGraphAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ToggleThreadedAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.options.SSCAEProjectUsageIntegrityOptions;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.options.resources.SSCAEProjectUsageIntegrityOptionsResources;
import gov.nasa.jpl.magicdraw.qvto.QVTOPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;
import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.io.CharStreams;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.options.EnvironmentOptions;
import com.nomagic.magicdraw.evaluation.EvaluationConfigurator;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.plugins.PluginDescriptor;
import com.nomagic.magicdraw.plugins.ResourceDependentPlugin;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 * 
 * @see https://support.nomagic.com/browse/MDUMLCS-8466
 * @see https://support.nomagic.com/browse/MDUMLCS-8495
 * @see https://support.nomagic.com/browse/MDUMLCS-8816
 * @see https://support.nomagic.com/browse/MDUMLCS-8857
 */
public class ProjectUsageIntegrityPlugin 
extends Plugin
implements ResourceDependentPlugin {
	
	public String getDOTexecutablePath() { 
		String dotCommand = options.getDotCommandProperty();
		return dotCommand;
	}
	
	public String getGraphvizApplicationPath() { 
		String graphvizApp = options.getGraphvizAppProperty();
		return graphvizApp;
	}

	public Double getRefreshRate(){
		return options.getStatusRefreshProperty();
	}
	
	public boolean isDotDefault(){
		Object defaultSelection = options.getDefaultCommandProperty();
		
		if (defaultSelection.equals(SSCAEProjectUsageIntegrityOptionsResources.getString(SSCAEProjectUsageIntegrityOptions.DEFAULT_DOT))){
			return true;
		}
		return false;
	}
	
	public boolean isPerformanceLoggingEnabled(){
		return options.getShowPerformanceStatusProperty();
	}
	
	public boolean isShowAdvancedInformationProperty(){
		return options.getShowAdvancedInformationProperty();
	}
	
	protected ProjectUsageSaveParticipant mSaveParticipant;
	protected ProjectUsageEventListenerAdapter mProjectEventListener;
	
	public ProjectUsageIntegrityHelper getSSCAEProjectUsageIntegrityProfileForProject(@Nonnull Project project) {
		return this.mProjectEventListener.getSSCAEProjectUsageIntegrityProfileForProject(project);
	}

	public final ToggleProjectUsageIntegrityCheckerAction projectUsageIntegrityToggleAction = new ToggleProjectUsageIntegrityCheckerAction();
	public final ShowCurrentProjectUsageGraphAction showCurrentProjectUsageGraphAction = new ShowCurrentProjectUsageGraphAction();
	public final CheckCurrentProjectUsageStatusAction checkCurrentProjectStatusAction = new CheckCurrentProjectUsageStatusAction();
	public final ToggleProjectUsageTeamworkOrAllGraphAction toggleTeamworkOrAllGraphAction = new ToggleProjectUsageTeamworkOrAllGraphAction();
	public final ToggleGraphLabelAction toggleGraphLabelAction = new ToggleGraphLabelAction();
	public final ToggleThreadedAction toggleThreadedAction = new ToggleThreadedAction();

	public boolean isProjectUsageIntegrityCheckerEnabled() { return projectUsageIntegrityToggleAction.getState(); }
	public boolean showLabelsOnGraph() { return toggleGraphLabelAction.getState(); }
	public boolean isThreadedMode(){ return toggleThreadedAction.getState(); }
	
	public static String PLUGIN_NAME = "SSCAE Project Usage Integrity Checker";
	
	private static ProjectUsageIntegrityPlugin INSTANCE = null;

	public static ProjectUsageIntegrityPlugin getInstance() { return INSTANCE; }

	protected Appender logTraceContractsAppender;
	
	public Appender getLogTraceContractsAppender() { return logTraceContractsAppender; }
	
	protected File logTraceContractsFolder;
	
	public File getLogTraceContractsFolder() { return logTraceContractsFolder; }
	
	/**
	 * http://docs.oracle.com/javase/6/docs/api/java/lang/System.html#getProperties()
	 * http://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html
	 */
	public static String[] JAVA_PROPERTIES = {
		"java.specification.version",
		"java.specification.name",
		"java.specification.vendor",
		"java.version",
		"java.vendor",
		"java.runtime.version",
		"java.runtime.name",
		"java.home",
		"os.name",
		"os.arch", 
		"os.version"
	};
	
	public static int MAX_PROPERTY_NAME_LENGTH = "java.specification.version".length();
	
	public static String PROPERTY_NAME_VALUE_FORMAT = "\n%-" + MAX_PROPERTY_NAME_LENGTH + "s=\"%s\"";
	
	public static final String SSCAE_CLEAN_MD5_TAG_PATTERN = "^SSCAE Clean Teamwork Commit \\{md5=[a-f0-9]*\\}$";
	public static Pattern SSCAE_CLEAN_MD5_TAG_REPLACE;
	 
	public static final String MD_TEAMWORK_PROJECT_ID_SUFFIXES_FILE = "MDTeamworkProjectIDSuffixes.txt";
	
	public final List<String> MDTeamworkProjectIDSuffixes = new ArrayList<String>();
	
	public SSCAEProjectUsageIntegrityOptions options;
	@Override
	public void init() {
		MDLog.getPluginsLog().info("INIT: >> " + getPluginName());
		try {
			String javaSpecificationVersion = System.getProperty("java.specification.version");
			if (!"1.6".equals(javaSpecificationVersion) && !"1.7".equals(javaSpecificationVersion)) {
				StringBuffer buff = new StringBuffer();
				buff.append("*** SSCAE supports running MagicDraw with Java 1.6 (recommended) or 1.7 at JPL ***\n");
				for (String property : JAVA_PROPERTIES) {
					buff.append(String.format(PROPERTY_NAME_VALUE_FORMAT, property, System.getProperty(property)));
				}
				
				buff.append("\n\nFor questions, contact SSCAE: https://sscae-help.jpl.nasa.gov/contacts.php");
				buff.append("\n\n*** OK to exit?");
				int okToExit = JOptionPane.showConfirmDialog(null, buff.toString(), "Java Version Mismatch", JOptionPane.OK_CANCEL_OPTION);
				if (okToExit == JOptionPane.OK_OPTION) {
					System.exit(-1);
				}
			}
			ProjectUsageIntegrityPlugin.INSTANCE = this;
			
			if (QVTOPlugin.getInstance() == null) {
				MDLog.getPluginsLog().fatal("The QVTO Library plugin should have been initialized!");
			}
			
			try {
				SSCAE_CLEAN_MD5_TAG_REPLACE = Pattern.compile(SSCAE_CLEAN_MD5_TAG_PATTERN);
			} catch (PatternSyntaxException e) {
				MDLog.getPluginsLog().fatal(String.format(
						"INIT: -- %s - cannot compile the SSCAE Clean tag regex pattern",
						getPluginName()), e);
				System.exit(-1);
			}
			
			this.mProjectEventListener = new ProjectUsageEventListenerAdapter(this);
			Application a = Application.getInstance();
			
			a.addProjectEventListener(this.mProjectEventListener);
			
			this.mSaveParticipant = new ProjectUsageSaveParticipant();
			a.addSaveParticipant(this.mSaveParticipant);
			
			EvaluationConfigurator.getInstance().registerBinaryImplementers(ProjectUsageIntegrityPlugin.class.getClassLoader());
			
			ActionsConfiguratorsManager manager = ActionsConfiguratorsManager.getInstance();
			manager.addMainToolbarConfigurator(new ToolbarConfigurator(this.projectUsageIntegrityToggleAction));
			manager.addMainToolbarConfigurator(new ToolbarConfigurator(this.checkCurrentProjectStatusAction));
			manager.addMainToolbarConfigurator(new ToolbarConfigurator(this.showCurrentProjectUsageGraphAction));
			manager.addMainToolbarConfigurator(new ToolbarConfigurator(this.toggleTeamworkOrAllGraphAction));
			manager.addMainToolbarConfigurator(new ToolbarConfigurator(this.toggleGraphLabelAction));
			manager.addMainToolbarConfigurator(new ToolbarConfigurator(this.toggleThreadedAction));

			final PluginDescriptor pluginDescriptor = this.getDescriptor();
			final String logTraceContractsDir = pluginDescriptor.getPluginDirectory() + File.separator + "logTraceContracts" + File.separator;
			logTraceContractsFolder = new File(logTraceContractsDir);
			if (!logTraceContractsFolder.exists() || !logTraceContractsFolder.isDirectory() || !logTraceContractsFolder.canRead()) {
				MDLog.getPluginsLog().error(String.format(
						"INIT: -- %s - cannot find the 'logTraceContracts' folder in: %s",
						getPluginName(), logTraceContractsDir));
				logTraceContractsFolder = null;
			}
			
			options = new SSCAEProjectUsageIntegrityOptions();
			EnvironmentOptions envOptions = a.getEnvironmentOptions();
			envOptions.addGroup(options);

			logTraceContractsAppender = new Appender();
			
			Logger.getRootLogger().addAppender(logTraceContractsAppender);
			System.setErr(new PrintStream(new AppendingOutputStream(System.err, logTraceContractsAppender, MDLog.getPluginsLog(), Level.ERROR)));	
			
			File pluginDir = pluginDescriptor.getPluginDirectory();
			File suffixFile = new File(pluginDir.getAbsolutePath() + File.separator + MD_TEAMWORK_PROJECT_ID_SUFFIXES_FILE);
			if (suffixFile.exists() && suffixFile.canRead()) {
				try {
					InputStream is = suffixFile.toURI().toURL().openStream();
					List<String> lines = CharStreams.readLines( new InputStreamReader( is ));
					for (String line : lines) {
						if (line.startsWith("#"))
							continue;
						if (line.isEmpty())
							continue;
						MDTeamworkProjectIDSuffixes.add(line);
						MDLog.getPluginsLog().info(String.format("MD teamwork project ID suffix: '%s'", line));
					}
				} catch (MalformedURLException e) {
					MDLog.getPluginsLog().error(String.format("Error reading the MD teamwork project ID suffix file: %s: %s", suffixFile, e.getMessage()), e);
					MDTeamworkProjectIDSuffixes.clear();
				} catch (IOException e) {
					MDLog.getPluginsLog().error(String.format("Error reading the MD teamwork project ID suffix file: %s: %s", suffixFile, e.getMessage()), e);
					MDTeamworkProjectIDSuffixes.clear();
				}
			}
		} finally {
			MDLog.getPluginsLog().info("INIT: << " + getPluginName());
		}
	}

	@Override
	public boolean close() {
		MDLog.getPluginsLog().info("CLOSE: >> " + getPluginName());
		try {
			Application.getInstance().removeProjectEventListener(this.mProjectEventListener);
			this.mProjectEventListener = null;

			return true;
		} finally {
			MDLog.getPluginsLog().info("CLOSE: << " + getPluginName());
		}
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public boolean isPluginRequired(Project p) {
		return (ProjectUtilities.findAttachedProjectByName(p, "SSCAEProjectUsageIntegrityProfile.mdzip") != null);
	}

	@Override
	public String getPluginName() {
		return getDescriptor().getName();
	}

	@Override
	public String getPluginVersion() {
		return getDescriptor().getVersion();
	}

}
