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

import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.logfire.RunnableSessionWrapperWithResult;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands.ComputeProjectUsageGraphCommand;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEUnloadedModuleAnnotation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.OperationCanceledException;

import com.nomagic.actions.NMAction;
import com.nomagic.ci.persistence.IProject;
import com.nomagic.magicdraw.actions.ActionsExecuter;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationManager;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.persistence.ProjectSaveService;
import com.nomagic.magicdraw.tests.MagicDrawTestCase;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public abstract class AbstractSSCAETest extends MagicDrawTestCase {

	protected ProjectUsageIntegrityPlugin projectUsageIntegrityPlugin;
	protected AnnotationManager annotationManager;
	protected Project testProject;
	protected ProjectUsageIntegrityHelper integrityHelper;
	protected Logger testLog;

	protected static List<String> REQUIRED_PLUGINS;
	
	static {
		REQUIRED_PLUGINS = new ArrayList<String>();
		REQUIRED_PLUGINS.add("AutomatonPlugin");
		REQUIRED_PLUGINS.add("com.nomagic.magicdraw.qvt");
		REQUIRED_PLUGINS.add("gov.nasa.jpl.magicdraw.qvto.library");
		REQUIRED_PLUGINS.add(ProjectUsageIntegrityPlugin.PLUGIN_ID);
	}
	
	@Override
	protected List<String> getRequiredPlugins() { return REQUIRED_PLUGINS; }
	
	protected static String getTestMethodNameFromTestProjectFile(@Nonnull File testProjectFile) {
		String filename = testProjectFile.getName();
		int i1 = filename.indexOf(".mdzip");
		if (i1 > 0)
			return filename.substring(0, i1);

		int i2 = filename.indexOf(".mdxml");
		if (i2 > 0)
			return filename.substring(0, i2);
		
		int i3 = filename.indexOf(".xml");
		if (i3 > 0)
			return filename.substring(0, i3);

		throw new IllegalArgumentException(String.format("getTestMethodNameFromTestProjectFile('%s') -- should end in .mdzip, .mdxml or .xml", testProjectFile));
	}
	
	protected static String getTestMethodNameFromTestProjectFile(@Nonnull String testProjectFile) {
		int i1 = testProjectFile.indexOf(".mdzip");
		if (i1 > 0)
			return testProjectFile.substring(0, i1);

		int i2 = testProjectFile.indexOf(".mdxml");
		if (i2 > 0)
			return testProjectFile.substring(0, i2);

		throw new IllegalArgumentException(String.format("getTestMethodNameFromTestProjectFile('%s') -- should end in .mdzip or .mdxml", testProjectFile));
	}

	public AbstractSSCAETest(@Nonnull String testMethod, @Nonnull String testName) {
		super(testMethod, testName);
		
		// checking memory leaks slows down considerably MD so it is turned off by default.
		setMemoryTestReady(false);
		setSkipMemoryTest(true);
	}

	@Override
	protected Project openProject(String projectFile) {

		testProject = super.openProject(projectFile);
		assertNotNull(testProject);

		projectUsageIntegrityPlugin = ProjectUsageIntegrityPlugin.getInstance();
		assertNotNull(projectUsageIntegrityPlugin);

		annotationManager = AnnotationManager.getInstance();
		assertNotNull(annotationManager);

		integrityHelper = projectUsageIntegrityPlugin.getSSCAEProjectUsageIntegrityProfileForProject(testProject);
		assertNotNull(integrityHelper);

		testLog = MDLog.getTestLog();
		assertNotNull(testLog);

		return testProject;
	}

	protected Project createProject(File templateFile, File projectFile){
		ActionsExecuter actionsExecuter = Application.getInstance().getActionsManager().getActionsExecuter();
		boolean success = actionsExecuter.newFromTemplate(templateFile.getAbsolutePath());
		if (!(success))
			return null;
		
		String filePath = projectFile.getAbsolutePath();
		
		ProjectSaveService.saveProjectOnTask(Application.getInstance().getProject(), filePath.substring(0,filePath.lastIndexOf(File.separator)), getTestMethodNameFromTestProjectFile(projectFile));
		testProject = Application.getInstance().getProject();
		assertNotNull(testProject);

		projectUsageIntegrityPlugin = ProjectUsageIntegrityPlugin.getInstance();
		assertNotNull(projectUsageIntegrityPlugin);

		annotationManager = AnnotationManager.getInstance();
		assertNotNull(annotationManager);

		integrityHelper = projectUsageIntegrityPlugin.getSSCAEProjectUsageIntegrityProfileForProject(testProject);
		assertNotNull(integrityHelper);

		testLog = MDLog.getTestLog();
		assertNotNull(testLog);

		return testProject;
	}

	public static final Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	protected boolean checkEmptyAnnotationsOrDescribeThem(StringBuffer buff, String prefix, Set<Annotation> annotations) {
		if (annotations.isEmpty())
			return true;

		for(Annotation a : annotations) {
			buff.append(String.format("\n%s: %s", prefix, describe(a)));
		}
		return false;
	}
	
	protected boolean checkEmptyAnnotationsExceptUnloadedModulesOrDescribeThem(StringBuffer buff, String prefix, Set<Annotation> annotations, boolean isTemplate) {
		if (annotations.isEmpty())
			return true;

		boolean ok = true;
		for(Annotation a : annotations) {
			if (a instanceof SSCAEUnloadedModuleAnnotation)
				continue;
			ok = false;
			buff.append(String.format("\n%s: %s", prefix, describe(a)));
		}
		return ok;
	}

	protected String describe(@Nonnull Annotation a) {
		StringBuffer buff = new StringBuffer();

		buff.append("Annotation{");

		String sLabel = (a.getSeverity() == null) ? "<N/A>" : a.getSeverity().getHumanName();
		buff.append(String.format("Annotation{severity=%s, kind='%s', ", sLabel, a.getKind()));

		BaseElement e = a.getTarget();
		if (null == e) 
			buff.append("target=<none>, ");
		else
			buff.append(String.format("target='%s' [type=%s, ID=%s], ", e.getHumanName(), e.getHumanType(), e.getID()));

		buff.append(String.format("text='%s'", a.getText()));

		List<? extends NMAction> actions = a.getActions();
		if (!actions.isEmpty()) {
			buff.append(", actions=[");
			boolean first=true;
			for (NMAction action : actions) {
				if (first)
					first = false;
				else
					buff.append(", ");
				buff.append(describe(action));
			}
			buff.append("]");
		}
		buff.append("}");
		return buff.toString();
	}

	protected String describe(@Nonnull NMAction a) {
		return String.format("Action{ID=%s, label=%s}", a.getID(), a.getName());
	}
	
	protected static String getSystemPropertyStringValue(String propertyName, String defaultValue) {
		String propertyValue = System.getProperty(propertyName, defaultValue);
        assertNotNull(
        		"System property: '" + propertyName + "' should be set", 
        		propertyValue);
        return propertyValue;
	}
	
	public static enum WritePermission { READ_ONLY, READ_WRITE };
	public static enum ExecutablePermission { NO_EXECUTE, EXECUTABLE };
	
	protected static File getSystemPropertyReadOnlyFileValue(String propertyName, String defaultFilePath) {
		return getSystemPropertyFileValue(propertyName, defaultFilePath, WritePermission.READ_ONLY, ExecutablePermission.NO_EXECUTE);
	}
	
	protected static File getSystemPropertyReadOnlyDirectoryValue(String propertyName, String defaultFilePath) {
		File dir = getSystemPropertyFileValue(propertyName, defaultFilePath, WritePermission.READ_ONLY, ExecutablePermission.EXECUTABLE);
		assertTrue("System property: '" + propertyName + "' should be a directory", dir.isDirectory());
		return dir;
	}
	
	protected static File getSystemPropertyModifiableDirectoryValue(String propertyName, String defaultFilePath) {
		File dir = getSystemPropertyFileValue(propertyName, defaultFilePath, WritePermission.READ_WRITE, ExecutablePermission.EXECUTABLE);
		assertTrue("System property: '" + propertyName + "' should be a directory", dir.isDirectory());
		return dir;
	}
	
	protected static File getSystemPropertyFileValue(String propertyName, String defaultFilePath, WritePermission canWrite, ExecutablePermission canExecute) {
		String filepath = getSystemPropertyStringValue(propertyName, defaultFilePath);
		File file = new File(filepath);
		assertTrue(
				"System property: '" + propertyName + "' should be an existing file",
				file.exists() && file.canRead());
		if (WritePermission.READ_WRITE == canWrite)
			assertTrue(
					"System property: '" + propertyName + "' should be a writeable file",
					file.canWrite());
		if (ExecutablePermission.EXECUTABLE == canExecute)
			assertTrue(
					"System property: '" + propertyName + "' should be an executable file",
					file.canExecute());
		return file;
	}
	
	public static int MAX_ITERATIONS = 100;
	
	protected final Logger log = MDLog.getTestLog();
	
	protected int iterations;
	protected int repairs;
	
	public void runSSCAEValidationAndRepairs(Project project, final String message) {
		SessionManager sessionManager = SessionManager.getInstance();
		if (sessionManager.isSessionCreated())
			sessionManager.closeSession();

		
		boolean showProjectUsageDiagnosticModalDialog = false;
		final ComputeProjectUsageGraphCommand c = new ComputeProjectUsageGraphCommand(project, showProjectUsageDiagnosticModalDialog);
		c.run();

		iterations = 0;
		repairs = 0;
		
		final IProject pp = project.getPrimaryProject();
		System.out.println(String.format("Before repairs (SSP=%b) %s", 
				ProjectUtilities.isStandardSystemProfile(pp), 
				c.getProjectUsageGraphDiagnostic()));
	 
		boolean checkForRepairs = true;
		while (checkForRepairs) {
			iterations++;
			checkForRepairs = new RunnableSessionWrapperWithResult<Boolean>(String.format("%s - (iteration=%d)", message, iterations)) {

				@Override
				public Boolean run() {
					assertTrue(message + " -- runSSCAEValidationAndRepairs -- too many iterations!", iterations < MAX_ITERATIONS);
					boolean check = false;

					Set<Annotation> annotations = integrityHelper.runSSCAEProjectStereotypeValidationRule();
					check = process(pp, check, annotations, message, "runSSCAEProjectStereotypeValidationRule");
					c.run();

					annotations = integrityHelper.runSSCAEValidProjectUsageGraphRule();
					check = process(pp, check, annotations, message, "runSSCAEValidProjectUsageGraphRule");
					c.run();

					annotations = integrityHelper.runSSCAEProjectUsageRelationshipRule();
					check = process(pp, check, annotations, message, "runSSCAEProjectUsageRelationshipRule");
					c.run();

					annotations = integrityHelper.runSSCAEProjectMD5ChecksumMismatchRule();
					check = process(pp, check, annotations, message, "runSSCAEProjectMD5ChecksumMismatchRule");
					c.run();

					return check;
				}
			}.execute();
		}

		System.out.println(String.format("After repairs (SSP=%b) %s", 
				ProjectUtilities.isStandardSystemProfile(pp), 
				c.getProjectUsageGraphDiagnostic()));
		
		String repairSummary = String.format("%s ==> %d repairs in %d iterations", message, repairs, iterations); 
		log.info(repairSummary);
		System.out.println(repairSummary);
	}

	public void runSSCAEValidationAndRepairsForMagicDrawSSPs(Project project, final String message) {
		SessionManager sessionManager = SessionManager.getInstance();
		if (sessionManager.isSessionCreated())
			sessionManager.closeSession();

		
		boolean showProjectUsageDiagnosticModalDialog = false;
		final ComputeProjectUsageGraphCommand c = new ComputeProjectUsageGraphCommand(project, !ProjectUsageIntegrityPlugin.getInstance().showLabelsOnGraph(), true, Collections.<String> emptySet(), showProjectUsageDiagnosticModalDialog);

		c.run();
		iterations = 0;
		repairs = 0;
		
		final IProject pp = project.getPrimaryProject();
		System.out.println(String.format("Before repairs (SSP=%b) %s", 
				ProjectUtilities.isStandardSystemProfile(pp), 
				c.getProjectUsageGraphDiagnostic()));
		
		boolean checkForRepairs = true;
		while (checkForRepairs) {
			iterations++;
			checkForRepairs = new RunnableSessionWrapperWithResult<Boolean>(String.format("%s - (iteration=%d)", message, iterations)) {

				@Override
				public Boolean run() {
					assertTrue(message + " -- runSSCAEValidationAndRepairs -- too many iterations!", iterations < MAX_ITERATIONS);
					boolean check = false;
					
					Set<Annotation> annotations = integrityHelper.runSSCAEValidProjectUsageGraphRule();
					check = process(pp, check, annotations, message, "runSSCAEValidProjectUsageGraphRule");
					c.run();

					annotations = integrityHelper.runSSCAEProjectUsageRelationshipRule();
					check = process(pp, check, annotations, message, "runSSCAEProjectUsageRelationshipRule");
					c.run();
					
					return check;
				}
			}.execute();

		}

		System.out.println(String.format("After repairs (SSP=%b) %s", 
				ProjectUtilities.isStandardSystemProfile(pp), 
				c.getProjectUsageGraphDiagnostic()));
		
		String repairSummary = String.format("%s ==> %d repairs in %d iterations", message, repairs, iterations); 
		log.info(repairSummary);
		System.out.println(repairSummary);
	}
	
	protected boolean process(IProject pp, boolean checkForRepairs, Set<Annotation> annotations, String message, String description) {
		if (null == annotations)
			return checkForRepairs;
		
		log.info(String.format("%s => %d annotations from %s", message, annotations.size(), description));
		for (Annotation a : annotations) {
			List<? extends NMAction> actions = a.getActions();
			log.info(String.format("==> %s", describe(a)));
			for (final NMAction action : actions) {
				repairs++;
				final String repairMessage = String.format("%s - (repair=%d) %s: %s", message, repairs, description, a.getText());
				System.out.println(repairMessage);
				new RunnableSessionWrapper(repairMessage) {

					@Override
					public void run() {
						action.actionPerformed(null);
					}
				};
				
				if (!integrityHelper.checkIProjectResources(pp))
					throw new OperationCanceledException(message + "\n*** Some project resources have lost their project relationship after sharing the package!");

				checkForRepairs = true;
			}
		}
		return checkForRepairs;
	}
}
