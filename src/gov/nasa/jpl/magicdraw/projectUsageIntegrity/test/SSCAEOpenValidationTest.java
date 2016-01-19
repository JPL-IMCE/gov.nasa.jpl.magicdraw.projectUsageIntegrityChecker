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
				skipTest |= testProjectFile.getName().endsWith(".bak");
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