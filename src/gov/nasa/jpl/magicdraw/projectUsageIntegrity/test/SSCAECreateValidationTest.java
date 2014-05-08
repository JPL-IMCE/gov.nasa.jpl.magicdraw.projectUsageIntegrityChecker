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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.tests.common.TestEnvironment;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 * @author Alek Kerzhner
 */
public class SSCAECreateValidationTest extends AbstractSSCAETest {

	protected final File testProjectFile;
	protected final File templateFile;
	protected static final String UML_TEMPLATE_FILE = "templates" + File.separator + "template.mdzip";
	protected static final String SYSML_TEMPLATE_FILE = "templates" + File.separator + "SysML" + File.separator + "SysML.mdzip";
	protected static final String UML_TEST_NAME = "test_umlProject_newproject_ok";
	protected static final String SYSML_TEST_NAME = "test_sysmlProject_newproject_ok";


	public SSCAECreateValidationTest(@Nonnull File templateFile, @Nonnull File testProjectFile, String testName) {
		super(testName, "SSCAECreateValidationTest." + testName);

		this.testProjectFile = testProjectFile;
		this.templateFile = templateFile;
		assertNotNull(testProjectFile);
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

	public void test_umlProject_newproject_ok() throws FileNotFoundException, IOException {
		createProject(templateFile, testProjectFile);

		assertTrue(integrityHelper.resolvedSSCAEProfileAndStereotypes());

		StringBuffer buff = new StringBuffer();

		boolean check1 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXECTED", integrityHelper.runSSCAEValidProjectUsageGraphRule());
		boolean check2 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXECTED", integrityHelper.runSSCAEProjectMD5ChecksumMismatchRule());
		//boolean check3 = cbeckEmptyAnnotationsOrDescribeThem(buff, "UNEXECTED", integrityHelper.runSSCAEProjectStereotypeValidationRule());
		boolean check4 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectUsageRelationshipRule());

		// During repair currently not adding SSCAE project stereotypes
		//assertTrue(buff.toString(), check1 && check2 && check3 && check4);
		assertTrue(buff.toString(), check1 && check2 && check4);
	}

	public void test_sysmlProject_newproject_ok() {
		createProject(templateFile, testProjectFile);

		assertTrue(integrityHelper.resolvedSSCAEProfileAndStereotypes());

		StringBuffer buff = new StringBuffer();
		
		boolean check1 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXECTED", integrityHelper.runSSCAEValidProjectUsageGraphRule());
		boolean check2 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXECTED", integrityHelper.runSSCAEProjectMD5ChecksumMismatchRule());
		//boolean check3 = cbeckEmptyAnnotationsOrDescribeThem(buff, "UNEXECTED", integrityHelper.runSSCAEProjectStereotypeValidationRule());
		boolean check4 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectUsageRelationshipRule());

		// During repair currently not adding SSCAE project stereotypes
		//assertTrue(buff.toString(), check1 && check2 && check3 && check4);
		assertTrue(buff.toString(), check1 && check2 && check4);
	}

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();
		String installRoot = ApplicationEnvironment.getInstallRoot();
		File resourceDir = TestEnvironment.getResourceDir();

		File skipProperties = new File(resourceDir.getAbsoluteFile() + File.separator + "skipTests.properties");
		Properties skipTests = new Properties();
		if (skipProperties.exists() && skipProperties.canRead()) {
			skipTests.load(new FileInputStream(skipProperties));
		}

		AbstractSSCAETest test = createTest(installRoot, resourceDir, skipTests, UML_TEST_NAME, UML_TEMPLATE_FILE);
		if (test != null)
			suite.addTest(test);
		
		test = createTest(installRoot, resourceDir, skipTests, SYSML_TEST_NAME, SYSML_TEMPLATE_FILE);
		if (test != null)
			suite.addTest(test);
		
		return suite;
	}

	public static AbstractSSCAETest createTest(String installRoot, File resourceDir, Properties skipTests, String testName, String templateFileLocation){
		Boolean skipTest = false;
		try {
			skipTest = Boolean.parseBoolean(skipTests.getProperty(testName, "false"));
			skipTest |= testName.endsWith(".bak");
		} catch (IllegalArgumentException e) {
		} catch (NullPointerException e) {
		}

		if (skipTest) {
			MDLog.getTestLog().info(String.format("Skipping test '%s'", testName));
			return null;
		} else {

			File templateFile = new File(installRoot + File.separator + templateFileLocation);
			File projectFile = new File(resourceDir + File.separator + "templates" + File.separator + testName + ".mdzip");

			return new SSCAECreateValidationTest(templateFile, projectFile, testName);
		}
	}
}
