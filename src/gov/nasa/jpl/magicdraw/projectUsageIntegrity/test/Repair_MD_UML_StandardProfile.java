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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;

/**
 * @author Nicolas F. Rouquette (JPL)
 * @author Alek Kerzhner (JPL)
 */
public class Repair_MD_UML_StandardProfile extends AbstractSSCAETest {

	public static final File installRootDir = getSystemPropertyModifiableDirectoryValue("install.root", "");
	public static final String installRootPath = installRootDir.getAbsolutePath();
	public static final String profilesRootPath = installRootPath + File.separator + "profiles" + File.separator;

	public static final File projectRootDir = getSystemPropertyModifiableDirectoryValue("project.root", "");
	public static final String projectRootPath = projectRootDir.getAbsolutePath();
	public static final String scriptPath = projectRootPath + "/scripts";
	public static final File scriptDir = new File(scriptPath);

	protected static final String UML_PROFILE_FILE = "UML_Standard_Profile.mdzip";
	protected static final String UML_PROFILE_NAME = "UML_Standard_Profile";

	public Repair_MD_UML_StandardProfile() {
		super("test_inject_SSCAEProjectUsageIntegrity_profile", "Inject_SSCAEProjectUsageIntegrity_profile");
	}

	public void test_inject_SSCAEProjectUsageIntegrity_profile() {
		inject();
		checkInjected();
	}
	
	protected void inject() {
		String profilesPath = ApplicationEnvironment.getProfilesDirectory();
		assertNotNull(profilesPath);

		String umlStandardProfilePath = profilesPath + UML_PROFILE_FILE;
		openProject(umlStandardProfilePath);

		File puiFile = new File(profilesRootPath + ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE_PROJECT_FILE);
		URI puiProfileURI = puiFile.toURI();
		String message = "Mounting " + ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE + " from: " + puiFile;
		log.info(message);
		ProjectDescriptor puiProfileDesc = ProjectDescriptorsFactory.createProjectDescriptor(puiProfileURI);

		ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
		boolean result = projectsManager.useModule(testProject, puiProfileDesc);
		assertTrue(message, result);

		Profile sscaePUI = StereotypesHelper.getProfile(testProject, ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE);
		assertNotNull(sscaePUI);
		
		Collection<Package> sharedPackages = ProjectUtilities.getSharedPackages(testProject.getPrimaryProject());
		
		List<Package> l = new ArrayList<Package>();
		l.addAll(sharedPackages);
		
		if (l.contains(sscaePUI)) {
			log.info("SSCAE PUI is already shared!");
		} else {
			log.info("Adding the SSCAE PUI as shared package!");
			l.add(sscaePUI);
		}
		
		projectsManager.sharePackage(testProject, l, "");
		
		runSSCAEValidationAndRepairsForMagicDrawSSPs(testProject, this.getName());

		ProjectDescriptor descriptor = ProjectDescriptorsFactory.getDescriptorForProject(testProject);
		boolean silent = true;
		boolean saved = projectsManager.saveProject(descriptor, silent);
		assertTrue(message, saved);
		
		closeAllProjects();
	}
	
	protected void checkInjected() {
		String profilesPath = ApplicationEnvironment.getProfilesDirectory();
		assertNotNull(profilesPath);

		String umlStandardProfilePath = profilesPath + UML_PROFILE_FILE;
		openProject(umlStandardProfilePath);
		
		if (! integrityHelper.resolvedSSCAEProfileAndStereotypes()) {
			List<Profile> profiles = StereotypesHelper.getAllProfiles(testProject);
			log.info(String.format("%s -- cannot resolve profile: '%s' -- but there are %d profiles", 
					this.getName(), ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE, profiles.size()));
			for (Profile pf : profiles) {
				log.info(String.format("%s -- profile: '%s' {ID=%s}", this.getName(), pf.getQualifiedName(), pf.getID()));
			}
		}
		
		assertTrue("check injected", integrityHelper.resolvedSSCAEProfileAndStereotypes());
	}


	@Override
	protected void setUpTest() throws Exception {
		super.setUpTest();
	}

	@Override
	protected void tearDownTest() throws Exception {
		super.tearDownTest();
		closeAllProjects();
	}

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();

		Repair_MD_UML_StandardProfile test = new Repair_MD_UML_StandardProfile();
		suite.addTest(test);

		return suite;
	}

}
