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

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands.ComputeProjectUsageGraphCommand;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.tests.common.TestEnvironment;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class SSCAESystemOrStandardProfileValidationTest extends AbstractSSCAETest {

	protected final File testProjectFile;
	protected Project testProject;
	protected static int totalTests;
	protected final int currentTest;
	protected final boolean isTemplate;
	public SSCAESystemOrStandardProfileValidationTest(@Nonnull File testProjectFile, boolean isTemplate, @Nonnull String testName, int currentTest) {
		super("test_SystemOrStandardProfile_ok", testName);
		this.testProjectFile = testProjectFile;
		assertNotNull(testProjectFile);
		this.currentTest = currentTest;
		this.isTemplate = isTemplate;
	}

	@Override
	protected void setUpTest() throws Exception {
		super.setUpTest();
		testProject = openProject(testProjectFile.getAbsolutePath());
	}
	
	@Override
	protected void tearDownTest() throws Exception {
		super.tearDownTest();
		closeAllProjects();
		
		System.gc();
		Runtime.getRuntime().runFinalization();
	}
	
	public void test_SystemOrStandardProfile_ok() {
		
		assertTrue(integrityHelper.resolvedSSCAEProfileAndStereotypes());
		integrityHelper.enable();
		String message = String.format("Checking %d of %d for: %s", currentTest, totalTests, testProjectFile.getAbsolutePath());
		log.info(message);
		System.out.println(message);
		
		boolean showProjectUsageDiagnosticModalDialog = false;

		ComputeProjectUsageGraphCommand c = new ComputeProjectUsageGraphCommand(testProject, showProjectUsageDiagnosticModalDialog);
		c.run();
		
		StringBuffer buff = new StringBuffer();
		buff.append(String.format("%s -- (%d/%d) '%s'\n", this.getName(), currentTest, totalTests, testProject.getName()));
		
		boolean check = checkEmptyAnnotationsExceptUnloadedModulesOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectUsageRelationshipRule(), this.isTemplate);
		
		assertTrue(buff.toString(), check);
	}
	
	protected static Comparator<File> FileComparator = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
		}
	};
	
	public static Test suite() throws Exception {
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
			
			String testID = "";
		
			if (counter < 10)
			   testID = String.format("SSCAESystemOrStandardProfileValidationTest_00%d", counter);
			else if (counter < 100)
				testID = String.format("SSCAESystemOrStandardProfileValidationTest_0%d", counter);
			else
				testID = String.format("SSCAESystemOrStandardProfileValidationTest_%d", counter);

			String testKeyName = testKey;
			
			testKeyName = testKeyName.replace('/', '_');
			testKeyName = testKeyName.replace('\\', '_');
			testKeyName = testKey.replaceFirst("\\.\\(mdzip\\|mdxml\\|xml\\.zip\\|xmi\\)","");
						
			suite.addTest(new SSCAESystemOrStandardProfileValidationTest(testFile, templates.contains(testFile), testID + testKeyName, counter));
		}
		return suite;
	}
}
