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
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.nomagic.magicdraw.tests.common.TestEnvironment;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class SSCAEOpenAuditTest extends AbstractSSCAETest {

	public static final String SSCAE_OPEN_AUDIT_MODELS_FROM_DIRECTORY = "audit.dir";
	public static final String SSCAE_OPEN_AUDIT_REPORT_PROPERTY = "audit.report";
	public static final String SSCAE_OPEN_AUDIT_REPORT_SERIALIZATION = "serialization";
	public static final String SSCAE_OPEN_AUDIT_REPORT_SIGNATURE = "signature";
	public static final String SSCAE_OPEN_AUDIT_REPORT_DESCRIPTION = String.format(
			"*** Possible values for '%s' are: '%s', '%s' (without quote marks)",
			SSCAE_OPEN_AUDIT_REPORT_PROPERTY, SSCAE_OPEN_AUDIT_REPORT_SERIALIZATION, SSCAE_OPEN_AUDIT_REPORT_SIGNATURE);
	
	public static enum AuditReportMode { SERIALIZATION, SIGNATURE };
	
	public static AuditReportMode getAuditReportMode(String mode) {
		if (null == mode) return null;
		if (SSCAE_OPEN_AUDIT_REPORT_SERIALIZATION.equalsIgnoreCase(mode)) return AuditReportMode.SERIALIZATION;
		if (SSCAE_OPEN_AUDIT_REPORT_SIGNATURE.equalsIgnoreCase(mode)) return AuditReportMode.SIGNATURE;
		return null;
	}
	
	protected final File testProjectFile;
	protected final AuditReportMode mode;
	
	public SSCAEOpenAuditTest(@Nonnull File testProjectFile, @Nonnull String testName, @Nonnull AuditReportMode mode) {
		super("test_OpenAuditModelFromDirectory", testName);
		this.testProjectFile = testProjectFile;
		this.mode = mode;
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
	
	public void test_OpenAuditModelFromDirectory() {
		if (integrityHelper.resolvedSSCAEProfileAndStereotypes()) {
			StringBuffer buff = new StringBuffer();
			boolean check1 = checkEmptyAnnotationsExceptProjectModelMD5ChecksumsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEValidProjectUsageGraphRule());
			boolean check2 = checkEmptyAnnotationsExceptProjectModelMD5ChecksumsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectMD5ChecksumMismatchRule());
			boolean check3 = checkEmptyAnnotationsExceptProjectModelMD5ChecksumsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectStereotypeValidationRule());
			boolean check4 = checkEmptyAnnotationsExceptProjectModelMD5ChecksumsOrDescribeThem(buff, "UNEXPECTED", integrityHelper.runSSCAEProjectUsageRelationshipRule());
			assertTrue(buff.toString(), check1 && check2 && check3 && check4);
		} else {
			assertNotNull(integrityHelper.latestProjectUsageGraph);
			String message = null;
			switch (mode) {
			case SERIALIZATION: 
				message = integrityHelper.latestProjectUsageGraph.getProjectUsageGraphSerialization(); 
				break;
			case SIGNATURE:
				message = integrityHelper.latestProjectUsageGraph.getProjectUsageGraphSignature(); 
				break;
			default:
				fail(SSCAE_OPEN_AUDIT_REPORT_DESCRIPTION);
				break;
			}
			assertNotNull(message);
			assertTrue(message, integrityHelper.latestProjectUsageGraph.isProjectUsageTopologyValid());
		}
	}
	
	public static Test suite() throws Exception {
		
		String userPath = System.getProperty("user.dir");
		assertNotNull(userPath);
		
		String auditPath = System.getProperty(SSCAE_OPEN_AUDIT_MODELS_FROM_DIRECTORY);
		if (null == auditPath) {
			System.out.println(String.format(
							"\n*** System property '%s' is unset ***\n*** Using current directory instead, i.e: '%s' ***\n",
							SSCAE_OPEN_AUDIT_MODELS_FROM_DIRECTORY, userPath));
			auditPath = userPath;
		}
		assertNotNull(auditPath);
		
		AuditReportMode mode = null;
		String reportMode = System.getProperty(SSCAE_OPEN_AUDIT_REPORT_PROPERTY);
		if (null == reportMode) {
			System.out.println(String.format(
					"\n*** System property '%s' is unset ***\n%s\n*** Using '%s' as a default value ***\n",
					SSCAE_OPEN_AUDIT_REPORT_PROPERTY, SSCAE_OPEN_AUDIT_REPORT_DESCRIPTION, SSCAE_OPEN_AUDIT_REPORT_SERIALIZATION));
			reportMode = SSCAE_OPEN_AUDIT_REPORT_SERIALIZATION;
		}
		mode = getAuditReportMode(reportMode);
		
		assertNotNull(
				String.format("\n*** '%s' is invalid for system property '%s' ***\n%s\n", 
						reportMode, SSCAE_OPEN_AUDIT_REPORT_PROPERTY, SSCAE_OPEN_AUDIT_REPORT_DESCRIPTION), 
				mode);
		
		TestSuite suite = new TestSuite();
		
		File resourceDir = TestEnvironment.getResourceDir();
		File skipProperties = new File(resourceDir.getAbsoluteFile() + File.separator + "skipOpenAudit.properties");
		Properties skipTests = new Properties();
		if (skipProperties.exists() && skipProperties.canRead()) {
			skipTests.load(new FileInputStream(skipProperties));
		}
		
		File auditDir = new File(auditPath);
		assertTrue(auditDir.exists());
		
		int auditPathPrefix = auditPath.length();
			
		List<File> allModels = TestEnvironment.getProjects(auditDir);
		for (File model : allModels) {
			String modelKey = model.getAbsolutePath().substring(auditPathPrefix);
				
			boolean skipTest = false;
			try {
				skipTest = Boolean.parseBoolean(skipTests.getProperty(modelKey, "false"));
			} catch (IllegalArgumentException e) {
			} catch (NullPointerException e) {
			}
        
			if (skipTest) {
				MDLog.getTestLog().info(String.format("Skipping test '%s'", model.getName()));
				continue;
			}
			suite.addTest(new SSCAEOpenAuditTest(model, modelKey, mode));
		}
		return suite;
	}
}
