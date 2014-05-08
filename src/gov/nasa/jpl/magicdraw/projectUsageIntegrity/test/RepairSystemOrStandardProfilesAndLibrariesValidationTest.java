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
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.modules.ModulesService;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.tests.common.TestEnvironment;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class RepairSystemOrStandardProfilesAndLibrariesValidationTest extends AbstractSSCAETest {

	protected final File testProjectFile;
	protected static int totalTests;
	protected final int currentTest;
	protected final boolean isTemplate;
	public RepairSystemOrStandardProfilesAndLibrariesValidationTest(@Nonnull File testProjectFile, boolean isTemplate, @Nonnull String testName, int currentTest) {
		super("test_repair_SystemOrStandardProfile", testName);
		this.testProjectFile = testProjectFile;
		assertNotNull(testProjectFile);
		this.currentTest = currentTest;
		this.isTemplate = isTemplate;
	}

	@Override
	protected void setUpTest() throws Exception {
		super.setUpTest();
		openProject(testProjectFile.getAbsolutePath());
	}
	
	@Override
	protected void tearDownTest() throws Exception {
		super.tearDownTest();
		closeAllProjects();
		
		System.gc();
		Runtime.getRuntime().runFinalization();
	}
	
	public void test_repair_SystemOrStandardProfile() {
		integrityHelper.enable();
		String message = String.format("Repairing %d of %d for: %s", currentTest, totalTests, testProjectFile.getAbsolutePath());
		log.info(message);
		System.out.println(message);
		String testInfo = String.format("%s -- (%d/%d) '%s'", this.getName(), currentTest, totalTests, testProject.getName());
		
		if (! integrityHelper.resolvedSSCAEProfileAndStereotypes()) {
			List<Profile> profiles = StereotypesHelper.getAllProfiles(testProject);
			log.info(String.format("%s -- cannot resolve profile: '%s' -- but there are %d profiles", 
					testInfo, ProjectUsageIntegrityHelper.SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE, profiles.size()));
			for (Profile pf : profiles) {
				log.info(String.format("%s -- profile: '%s' {ID=%s}", testInfo, pf.getQualifiedName(), pf.getID()));
			}
		}
		
		assertTrue(testInfo, integrityHelper.resolvedSSCAEProfileAndStereotypes());
		runSSCAEValidationAndRepairsForMagicDrawSSPs(testProject, this.getName());

		if (isTemplate) {
			if (ProjectUtilities.isStandardSystemProfile(testProject.getPrimaryProject())) {
				String ssp = String.format("%s *** SSP flag needs to be set to false for a template!", testInfo); 
				log.warn(ssp);
				System.out.println(ssp);
				ModulesService.setStandardSystemProfile(testProject.getPrimaryProject(), false);
				if (repairs == 0) repairs++;
			} else {
				String ssp = String.format("%s --- SSP flag is already false for this template", testInfo);
				log.info(ssp);
				System.out.println(ssp);
			}
		} else {
			if (ProjectUtilities.isStandardSystemProfile(testProject.getPrimaryProject())) {
				String ssp = String.format("%s --- SSP flag is already set to true", testInfo);
				log.info(ssp);
				System.out.println(ssp);
			} else {
				String ssp = String.format("%s *** SSP flag needs to be set to true!", testInfo);
				log.warn(ssp);
				System.out.println(ssp);
				ModulesService.setStandardSystemProfile(testProject.getPrimaryProject(), true);
				if (repairs == 0) repairs++;
			}
		}
		
		if (repairs > 0) {
			log.info(String.format("Saving '%s' because %d repairs in %d iterations were made", testProject.getName(), repairs, iterations));
			
			ProjectDescriptor descriptor = ProjectDescriptorsFactory.getDescriptorForProject(testProject);
		
			// It is possible that some profile/library depends on another profile/library that has not been repaired yet.
			// 1st pass: repair everything but disables the PUI checker so that we can save the repair, even if it is partial (i.e., invalid dependencies)
			// 2nd pass: check everything is OK (this is not done in this test)
			integrityHelper.disable();
			
			ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
			boolean silent = true;
			boolean saved = projectsManager.saveProject(descriptor, silent);
			assertTrue(testInfo, saved);
		}
	}
	
	protected static Comparator<File> FileComparator = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
		}
	};
	
	public static Test suite() throws Exception {
		
		String testStartValue = getSystemPropertyStringValue("test.start", "");
		assertTrue(null != testStartValue && testStartValue.length() > 0);
		int testStart = Integer.parseInt(testStartValue);
		
		String testCountValue = getSystemPropertyStringValue("test.count", "");
		assertTrue(null != testCountValue && testCountValue.length() > 0);
		int testCount = Integer.parseInt(testCountValue);
		
		totalTests = 0;
		TestSuite suite = new TestSuite();
		
		File resourceDir = TestEnvironment.getResourceDir();
		File skipProperties = new File(resourceDir.getAbsoluteFile() + File.separator + "skipProfiles.properties");
		Properties skipTests = new Properties();
		if (skipProperties.exists() && skipProperties.canRead()) {
			skipTests.load(new FileInputStream(skipProperties));
		}
		
		String installRootPath = ApplicationEnvironment.getInstallRoot();
		if (!installRootPath.endsWith(File.separator))
			installRootPath.concat(File.separator);
		int installRootPathPrefix = installRootPath.length();
		
		String profilesPath = installRootPath + "profiles" + File.separator;
		int profilesPathPrefix = profilesPath.length();
		
		File profilesDir = new File(profilesPath);
		assertTrue(profilesDir.exists());
		
		List<File> allFiles = TestEnvironment.getProjects(profilesDir);
		Collections.sort(allFiles, FileComparator);
		
		String templatesPath = installRootPath + "templates" + File.separator;
		int templatesPathPrefix = templatesPath.length();
		
		File templatesDir = new File(templatesPath);
		List<File> templates = TestEnvironment.getProjects(templatesDir);
		Collections.sort(templates, FileComparator);
		
		allFiles.addAll(templates);
		
		String modelLibrariesPath = installRootPath + "modelLibraries" + File.separator;
		int modelLibrariesPathPrefix = modelLibrariesPath.length();
		
		File modelLibrariesDir = new File(modelLibrariesPath);
		List<File> libraries = TestEnvironment.getProjects(modelLibrariesDir);
		Collections.sort(libraries, FileComparator);
		
		allFiles.addAll(libraries);
		
		int counter = 0;
		int rangeCount = 0;
		
		for (File testFile : allFiles) {
			boolean skipTest = false;
			String testKey = "";
			String testFilePath = testFile.getAbsolutePath();
			if (testFilePath.startsWith(profilesPath)) {
				testKey = testFilePath.substring(profilesPathPrefix);
			} else if (testFilePath.startsWith(modelLibrariesPath)) {
				testKey = testFilePath.substring(modelLibrariesPathPrefix);
			} else if (testFilePath.startsWith(templatesPath)) {
				testKey = testFilePath.substring(templatesPathPrefix);
			} else if (testFilePath.startsWith(installRootPath)) {
				testKey = testFilePath.substring(installRootPathPrefix);
			}
			try {	
				skipTest = Boolean.parseBoolean(skipTests.getProperty(testKey, "false"));
				skipTest |= testKey.endsWith(".bak");
			} catch (IllegalArgumentException e) {
			} catch (NullPointerException e) {
			}
        
			if (skipTest) {
				MDLog.getTestLog().info(String.format("Skipping test '%s'", testFile.getName()));
				continue;
			}
			
			totalTests++;
			
			counter++;
			if (counter < testStart)
				continue;
			
			rangeCount++;
			if (rangeCount > testCount)
				continue;
			
			String testID = "";
			
			if (counter < 10)
			   testID = String.format("RepairSystemOrStandardProfilesAndLibrariesValidationTest_00%d", counter);
			else if (counter < 100)
				testID = String.format("RepairSystemOrStandardProfilesAndLibrariesValidationTest_0%d", counter);
			else
				testID = String.format("RepairSystemOrStandardProfilesAndLibrariesValidationTest_%d", counter);

			String testKeyName = testKey.replace('/', '_');
			testKeyName = testKeyName.replace('\\', '_');
			testKeyName = testKeyName.replaceFirst("\\.\\(mdzip\\|mdxml\\|xml\\.zip\\|xmi\\)","");
			
			suite.addTest(new RepairSystemOrStandardProfilesAndLibrariesValidationTest(testFile, templates.contains(testFile), testID + testKeyName, counter));
		}
		return suite;
	}
}
