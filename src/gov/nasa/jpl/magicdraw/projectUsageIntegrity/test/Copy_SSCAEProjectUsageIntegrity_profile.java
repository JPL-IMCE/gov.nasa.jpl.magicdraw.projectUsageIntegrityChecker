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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.test;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.plugins.PluginDescriptor;
import com.nomagic.magicdraw.plugins.PluginUtils;

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
	public static Collection<Plugin> getStartedMDPlugins() {
		return PluginUtils.getPlugins();
	}
	
	@Override
	protected void setUpTest() throws Exception {
		super.setUpTest();
		
		Collection<Plugin> startedPlugins = getStartedMDPlugins();
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