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

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.tests.common.TestEnvironment;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.utils.Utilities;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class SSCAEOpenValidationTest extends AbstractSSCAETest {

	protected final File testProjectFile;
	
	public SSCAEOpenValidationTest(@Nonnull File testProjectFile) {
		super(getTestMethodNameFromTestProjectFile(testProjectFile), "SSCAEOpenValidationTest." + getTestMethodNameFromTestProjectFile(testProjectFile));
		this.testProjectFile = testProjectFile;
		assertNotNull(testProjectFile);
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
	}
	
	public void test_umlProject_acyclic_consistent_valid_1MissingLocalUsage() throws FileNotFoundException, IOException {
		assertTrue(integrityHelper.resolvedSSCAEProfileAndStereotypes());
		
		SSCAEProjectUsageGraph g = integrityHelper.latestProjectUsageGraph;
		assertNotNull(g);
		
		String dx = g.getProjectUsageGraphSerialization();
		String dxFilepath = testProjectFile.getAbsolutePath().replaceFirst(".mdzip", ".txt");
		File testProjectDxFile = new File(dxFilepath);
		assertTrue(testProjectDxFile.exists() && testProjectDxFile.canRead());
		
		String testProjectDx = Utilities.toString(new FileInputStream(testProjectDxFile));
		
		assertEquals(dx, testProjectDx);
	}
	
	public void test_umlProject_acyclic_consistent_valid_ok() {
		assertTrue(integrityHelper.resolvedSSCAEProfileAndStereotypes());
		
		StringBuffer buff = new StringBuffer();
		
		boolean check1 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEValidProjectUsageGraphRule());
		boolean check2 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectMD5ChecksumMismatchRule());
		boolean check3 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectStereotypeValidationRule());
		boolean check4 = checkEmptyAnnotationsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectUsageRelationshipRule());
		
		assertTrue(buff.toString(), check1 && check2 && check3 && check4);
	}
	
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();
		File resourceDir = TestEnvironment.getResourceDir();
		File skipProperties = new File(resourceDir.getAbsoluteFile() + File.separator + "skipTests.properties");
		Properties skipTests = new Properties();
		if (skipProperties.exists() && skipProperties.canRead()) {
			skipTests.load(new FileInputStream(skipProperties));
		}
		
		List<File> testProjectFiles = TestEnvironment.getProjects("local");
		Collections.sort(testProjectFiles, FILE_COMPARATOR);
		for (File testProjectFile : testProjectFiles) {
			boolean skipTest = false;
			try {
				skipTest = Boolean.parseBoolean(skipTests.getProperty(testProjectFile.getName(), "false"));
			} catch (IllegalArgumentException e) {
			} catch (NullPointerException e) {
			}
        
			if (skipTest) {
				MDLog.getTestLog().info(String.format("Skipping test '%s'", testProjectFile.getName()));
				continue;
			}
			suite.addTest(new SSCAEOpenValidationTest(testProjectFile));
		}
		return suite;
	}
}
