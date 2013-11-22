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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.test;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.plugins.PluginDescriptor;

/**
 * @author Nicolas F. Rouquette (JPL)
 * @author Alek Kerzhner (JPL)
 */
public class Copy_SSCAEProjectUsageIntegrity_profile extends AbstractSSCAETest {

	public static final File installRootDir = getSystemPropertyModifiableDirectoryValue("install.root", "");
	public static final String installRootPath = installRootDir.getAbsolutePath();
	public static final String profilesRootPath = installRootPath + File.separator + "profiles" + File.separator;

	public static final File projectRootDir = getSystemPropertyModifiableDirectoryValue("project.root", "");
	public static final String projectRootPath = projectRootDir.getAbsolutePath();
	public static final String scriptPath = projectRootPath + "/scripts";
	public static final File scriptDir = new File(scriptPath);

	protected static final String UML_PROFILE_FILE = "UML_Standard_Profile.mdzip";
	protected static final String UML_PROFILE_NAME = "UML_Standard_Profile";

	public Copy_SSCAEProjectUsageIntegrity_profile() {
		super("test_copy_SSCAEProjectUsageIntegrity_profile", "Copy_SSCAEProjectUsageIntegrity_profile");

		String profilesPath = ApplicationEnvironment.getProfilesDirectory();
		assertNotNull(profilesPath);

	}

	public void test_copy_SSCAEProjectUsageIntegrity_profile() {
		String sourcePUIfile = projectRootPath + File.separator + "profiles" + File.separator + ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE_PROJECT_FILE;
		Project project = openProject(sourcePUIfile);
		runSSCAEValidationAndRepairs(project, this.getName());

		File puiFile = new File(profilesRootPath + ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE_PROJECT_FILE);

		String message = "Copying " + ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE + " to: " + puiFile;
		log.info(message);
		saveProject(project, puiFile);

		SSCAEProjectUsageIntegrity_profile_descriptor = ProjectDescriptorsFactory.getDescriptorForProject(project);
	}

	public ProjectDescriptor SSCAEProjectUsageIntegrity_profile_descriptor;

	/**
	 * Copied from the QVTOLib plugin's QVTOUtils to avoid a plugin dependency.
	 * @return
	 */
	public static List<Plugin> getStartedMDPlugins() {
		return Application.getInstance().getPluginManager().B();
	}
	
	@Override
	protected void setUpTest() throws Exception {
		super.setUpTest();
		
		List<Plugin> startedPlugins = getStartedMDPlugins();
		Plugin puiPlugin = null;
		PluginDescriptor puiPD = null;
		
		for (Plugin p : startedPlugins) {
			PluginDescriptor pd = p.getDescriptor();
			String name = pd.getName();
			if (ProjectUsageIntegrityPlugin.PLUGIN_NAME.equals(name)) {
				puiPlugin = p;
				puiPD = pd;
				break;
			}
		}
		assertNotNull("Cannot find plugin: '" + ProjectUsageIntegrityPlugin.PLUGIN_NAME + "'", puiPlugin);
		assertNotNull("Cannot find plugin: '" + ProjectUsageIntegrityPlugin.PLUGIN_NAME + "'", puiPD);
		assertTrue("plugin should be enabled: '" + ProjectUsageIntegrityPlugin.PLUGIN_NAME + "'", puiPD.isEnabled());
		assertTrue("plugin should be loaded: '" + ProjectUsageIntegrityPlugin.PLUGIN_NAME + "'", puiPD.isLoaded());
	}

	@Override
	protected void tearDownTest() throws Exception {
		super.tearDownTest();
		closeAllProjects();
	}

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();

		Copy_SSCAEProjectUsageIntegrity_profile test = new Copy_SSCAEProjectUsageIntegrity_profile();
		suite.addTest(test);

		return suite;
	}

}
