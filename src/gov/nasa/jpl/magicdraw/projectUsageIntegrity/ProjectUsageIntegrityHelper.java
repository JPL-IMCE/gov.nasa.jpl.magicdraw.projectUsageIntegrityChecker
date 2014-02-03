package gov.nasa.jpl.magicdraw.projectUsageIntegrity;
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

import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.logfire.SessionReport;
import gov.nasa.jpl.magicdraw.log.Log;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ApplyAllSSCAERepairs;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ApplyStereotypeAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.RepairAddMissingUsageRelationshipAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.RepairProjectUsageToReSharedAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.RepairProjectUsageToReadOnlyAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.RepairProjectUsageToStickyVersionAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.RepairSystemOrStandardProfileFlagAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.RepairUnloadedModuleWithProxiesAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.ToggleProjectUsageIntegrityCheckerAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions.UnapplyStereotypeAction;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands.ComputeProjectUsageGraphCommand;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDAbstractProject;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDAbstractProjectUsage;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDAttachedProject;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDLocalAttachedProject;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDLocalPrimaryProject;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDTeamworkAttachedProject;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDTeamworkPrimaryProject;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDTeamworkProjectUsage;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.ProjectClassification;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.ProjectClassificationCharacteristics;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectDigest;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.YamlDigestHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEAnnotation;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEProjectMD5ChecksumMismatchValidation;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEProjectStereotypeValidation;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEProjectUsageRelationshipValidation;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEUnloadedModuleAnnotation;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEValidProjectUsageGraphValidation;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;

import junit.framework.ComparisonFailure;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.yaml.snakeyaml.error.YAMLException;

import com.nomagic.actions.NMAction;
import com.nomagic.ci.metamodel.project.ProjectUsage;
import com.nomagic.ci.persistence.local.decomposition.DecompositionEvent;
import com.nomagic.ci.persistence.local.ExtractProjectEvent;
import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.IProjectRepository;
import com.nomagic.ci.persistence.local.ProjectAttachOrDettachEvent;
import com.nomagic.ci.persistence.local.ProjectConfigurationException;
import com.nomagic.ci.persistence.ProjectEvent;
import com.nomagic.ci.persistence.ProjectEventType;
import com.nomagic.ci.persistence.ProjectListener;
import com.nomagic.ci.persistence.local.ProjectRepositoryRegistry;
import com.nomagic.ci.persistence.decomposition.ProjectAttachmentConfiguration;
import com.nomagic.ci.persistence.local.spi.decomposition.IDecompositionModel;
import com.nomagic.ci.persistence.local.spi.decomposition.IProjectUsageManager;
import com.nomagic.ci.persistence.local.spi.localproject.ILocalProjectInternal;
import com.nomagic.ci.persistence.local.spi.localproject.LocalAttachedProject;
import com.nomagic.ci.persistence.local.spi.localproject.LocalPrimaryProject;
import com.nomagic.ci.persistence.local.versioning.Versioning;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.teamwork.application.storage.ITeamworkProject;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkAttachedProject;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkPrimaryProject;
import com.nomagic.magicdraw.uml.DiagramType;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.magicdraw.validation.ElementValidationRuleImpl;
import com.nomagic.magicdraw.validation.RuleViolationResult;
import com.nomagic.magicdraw.validation.ValidationRunData;
import com.nomagic.magicdraw.validation.ValidationSuiteHelper;
import com.nomagic.magicdraw.validation.ui.ValidationResultsWindowManager;
import com.nomagic.uml2.ext.jmi.EventSupport;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.utils.Utilities;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ProjectUsageIntegrityHelper implements ProjectListener {

	public final static String SSCAE_PROJECT_USAGE_VALIDATION_SUITE_QNAME = "SSCAE ProjectUsage Integrity Profile::ProjectUsage Validation Suite";

	public static Package getSSCAEProjectUsageValidationSuite(@Nonnull Project project) {
		ValidationSuiteHelper vsh = ValidationSuiteHelper.getInstance(project);
		if (null == vsh)
			return null;

		for (Package p : vsh.getValidationSuites()) {
			if (SSCAE_PROJECT_USAGE_VALIDATION_SUITE_QNAME.equals(p.getQualifiedName())) {
				return p;
			}
		}
		return null;
	}

	public static Package locateSSCAEProjectUsageValidationSuite(@Nonnull Project project) {
		Package suite = getSSCAEProjectUsageValidationSuite(project);
		if (suite == null) {
			MDLog.getPluginsLog().error(String.format(
					"ProjectUsageIntegrityHelper.runSSCAEValidationAndShowResults: resolved SSCAE profile & stereotypes but cannot find it by its qualified name: '%s'",
					SSCAE_PROJECT_USAGE_VALIDATION_SUITE_QNAME));
			
		}
		return suite;
	}
	
	public final static String SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE_PROJECT_FILE = "SSCAEProjectUsageIntegrityProfile.mdzip";
	public final static String SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE = "SSCAE ProjectUsage Integrity Profile";
	public final static String SSCAE_ABSTRACT_USAGE_STEREOTYPE_NAME = "AbstractSSCAEProjectUsageGraph";
	public final static String SSCAE_SHARED_PACKAGE_STEREOTYPE_NAME = "SSCAESharedPackage";
	public final static String SSCAE_PROJECT_MODEL_STEREOTYPE_NAME = "SSCAEProjectModel";

	public final static String SSCAE_PROJECT_STEREOTYPE_GRAPH_VERSION = "_sscaeProjectUsageGraphVersion";
	public final static String SSCAE_PROJECT_STEREOTYPE_GRAPH_SERIALIZATION = "_sscaeProjectUsageGraphSerialization";

	public void enable() {
		checkerState.setState(true);
		checkerState.updateState();
	}
	
	public void disable() {
		checkerState.setState(false);
	}
	
	public boolean isEnabled() {
		return checkerState.getState();
	}

	public final SSCAEProjectRepositoryListener prListener;
			
	public ProjectUsageIntegrityHelper(@Nonnull Project project, ToggleProjectUsageIntegrityCheckerAction checkerState) {
		this.project = project;
		this.pProject = project.getPrimaryProject();
		this.logger = MDLog.getPluginsLog();
		this.checkerState = checkerState;
		this.errorLevel = ValidationSuiteHelper.getInstance(project).getSeverityLevel("error");
		this.warningLevel = ValidationSuiteHelper.getInstance(project).getSeverityLevel("warning");
		
		this.pProject.addProjectListener(this);
		this.prListener = new SSCAEProjectRepositoryListener(this);
		
		ProjectRepositoryRegistry prr = ProjectRepositoryRegistry.getInstance();
		IPrimaryProject pp = project.getPrimaryProject();
		URI uri = pp.getLocationURI();
		IProjectRepository pr = prr.getProjectRepository(uri);
		if (pr != null)
			pr.addListener(prListener);
		
		SSCAEProjectDigest digest = null;
		
		if (this.resolvedSSCAEProfileAndStereotypes()) {
			String serialization = this.getSSCAEProjectModelGraphSerialization(this.project.getModel());
			if (serialization != null && !serialization.isEmpty()) {
				try {
					digest = YamlDigestHelper.load(serialization);
				} catch (YAMLException ye) {
					digest = null;
				}
			}
		}
		this.projectDigest = digest;
	}

	public void dispose() {
	}

	public final Project project;
	public final SSCAEProjectDigest projectDigest;
	
	public MDAbstractProject getPreviousProjectVertexMatching(MDAbstractProject v) {
		if (projectDigest == null)
			return null;
		
		String vID = v.getProjectID();
		for (MDAbstractProject pv : projectDigest.getAllSortedProjects()) {
			if (vID.equals(pv.getProjectID())) {
				return pv;
			}
		}
		
		return null;
	}
	
	public final IPrimaryProject pProject;
	public final Logger logger;
	protected final ToggleProjectUsageIntegrityCheckerAction checkerState;
	
	protected final Boolean[] reportProjectConfigurationException = { true };
	
	public ProjectClassification getProjectClassification() {
		
		ProjectClassification _pClassification = null;
		try {
			ProjectClassificationCharacteristics pc = new ProjectClassificationCharacteristics(this.project);
			_pClassification = pc.projectClassification;
		} catch (ProjectConfigurationException e) {
			if (reportProjectConfigurationException[0]) {
				reportProjectConfigurationException[0] = false;
				String message = String.format("ProjectUsageIntegrityHelper: cannot analyze project '%s' {ID=%s} because its Project Usage metadata is invalid: %s",
						this.project.getName(), this.project.getID(), e.getMessage());

				Application.getInstance().getGUILog().clearLog();
				Log.log(message);

				logger.error(message, e);
			}
			_pClassification = ProjectClassification.INVALID;
		}
		return _pClassification;
	}
	
	protected final EnumerationLiteral errorLevel;
	protected final EnumerationLiteral warningLevel;

	public EnumerationLiteral getValidationErrorLevel() {
		return errorLevel;
	}

	public EnumerationLiteral getValidationWarningLevel() {
		return warningLevel;
	}

	protected Profile sscaeProjectUsageIntegrityProfile;
	public boolean hasSSCAEProjectUsageIntegrityProfile() { return sscaeProjectUsageIntegrityProfile != null; }
	
	protected Stereotype sscaeAbstractUsageStereotype;
	protected Stereotype sscaeSharedPackageStereotype;
	protected Stereotype sscaeProjectModelStereotype;

	public static String SSCAE_PROJECT_USAGE_VALIDATION_SUITE_NAME = "ProjectUsage Validation Suite";
	protected Package sscaeProjectUsageValidationSuite;

	protected final Set<Constraint> validationSuiteConstraints = new HashSet<Constraint>();

	public Collection<Constraint> getSSCAEValidationConstraints() {
		return Collections.unmodifiableSet(validationSuiteConstraints);
	}

	public ValidationRunData createSSCAEValidationRunData() {
		return new ValidationRunData(project.getModel(), false, getSSCAEValidationConstraints(), getValidationErrorLevel());
	}

	//

	public static String SSCAE_PROJECT_USAGE_RELATIONSHIP_RULE_NAME = "SSCAEProjectUsageRelationshipValidation";
	public static java.lang.Class<?>[] sscaeProjectUsageRelationshipMetaclasses = new java.lang.Class<?>[] {Model.class};
	public Constraint sscaeProjectUsageRelationshipConstraint;
	protected ElementValidationRuleImpl sscaeProjectUsageRelationshipRule;

	public ElementValidationRuleImpl getSSCAEProjectUsageRelationshipRule() {
		if (null == sscaeProjectUsageRelationshipRule) {
			sscaeProjectUsageRelationshipRule = new SSCAEProjectUsageRelationshipValidation();
		}
		sscaeProjectUsageRelationshipRule.init(project, sscaeProjectUsageRelationshipConstraint);
		return sscaeProjectUsageRelationshipRule;
	}

	public Set<Annotation> runSSCAEProjectUsageRelationshipRule() {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("runSSCAEProjectUsageRelationshipRule() -- the SSCAE profile and stereotypes are not yet resolved");

		return getSSCAEProjectUsageRelationshipRule().run(
				project, 
				sscaeProjectUsageRelationshipConstraint,
				ModelHelper.getElementsOfType(project.getModel(), sscaeProjectUsageRelationshipMetaclasses, true));
	}

	//

	public static String SSCAE_VALID_PROJECT_USAGE_GRAPH_RULE_NAME = "SSCAEValidProjectUsageGraph";
	public static java.lang.Class<?>[] sscaeValidProjectUsageGraphMetaclasses = new java.lang.Class<?>[] {Package.class, Model.class};
	public Constraint sscaeValidProjectUsageGraphConstraint;
	protected ElementValidationRuleImpl sscaeValidProjectUsageGraphRule;

	public ElementValidationRuleImpl getSSCAEValidProjectUsageGraphRule() {
		if (null == sscaeValidProjectUsageGraphRule) {
			sscaeValidProjectUsageGraphRule = new SSCAEValidProjectUsageGraphValidation();
		}
		sscaeValidProjectUsageGraphRule.init(project, sscaeValidProjectUsageGraphConstraint);
		return sscaeValidProjectUsageGraphRule;
	}

	public Set<Annotation> runSSCAEValidProjectUsageGraphRule() {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("runSSCAEValidProjectUsageGraphRule() -- the SSCAE profile and stereotypes are not yet resolved");

		return getSSCAEValidProjectUsageGraphRule().run(
				project, 
				sscaeValidProjectUsageGraphConstraint,
				ModelHelper.getElementsOfType(project.getModel(), sscaeValidProjectUsageGraphMetaclasses, true));
	}

	//

	public static String SSCAE_PROJECT_STEREOTYPE_VALIDATION_RULE_NAME = "SSCAEProjectStereotypeValidation";
	public static java.lang.Class<?>[] sscaeProjectStereotypeValidationMetaclasses = new java.lang.Class<?>[] {Package.class};
	public Constraint sscaeProjectStereotypeValidationConstraint;
	protected ElementValidationRuleImpl sscaeProjectStereotypeValidationRule;

	public ElementValidationRuleImpl getSSCAEProjectStereotypeValidationRule() {
		if (null == sscaeProjectStereotypeValidationRule) {
			sscaeProjectStereotypeValidationRule = new SSCAEProjectStereotypeValidation();
		}
		sscaeProjectStereotypeValidationRule.init(project, sscaeProjectStereotypeValidationConstraint);
		return sscaeProjectStereotypeValidationRule;
	}

	public Set<Annotation> runSSCAEProjectStereotypeValidationRule() {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("runSSCAEProjectStereotypeValidationRule() -- the SSCAE profile and stereotypes are not yet resolved");

		return getSSCAEProjectStereotypeValidationRule().run(
				project, 
				sscaeProjectStereotypeValidationConstraint,
				ModelHelper.getElementsOfType(project.getModel(), sscaeProjectStereotypeValidationMetaclasses, true));
	}

	//

	public static String SSCAE_PROJECT_MD5_CHECKSUM_MISMATCH_RULE_NAME = "SSCAEProjectMD5ChecksumMismatch";
	public static java.lang.Class<?>[] sscaeProjectMD5ChecksumMismatchMetaclasses = new java.lang.Class<?>[] {Model.class};
	public Constraint sscaeProjectMD5ChecksumMismatchConstraint;
	protected ElementValidationRuleImpl sscaeProjectMD5ChecksumMismatchRule;

	public ElementValidationRuleImpl getSSCAEProjectMD5ChecksumMismatchRule() {
		if (null == sscaeProjectMD5ChecksumMismatchRule) {
			sscaeProjectMD5ChecksumMismatchRule = new SSCAEProjectMD5ChecksumMismatchValidation();
		}
		sscaeProjectMD5ChecksumMismatchRule.init(project, sscaeProjectMD5ChecksumMismatchConstraint);
		return sscaeProjectMD5ChecksumMismatchRule;
	}

	public Set<Annotation> runSSCAEProjectMD5ChecksumMismatchRule() {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("runSSCAEProjectMD5ChecksumMismatchRule() -- the SSCAE profile and stereotypes are not yet resolved");

		return getSSCAEProjectMD5ChecksumMismatchRule().run(
				project, 
				sscaeProjectMD5ChecksumMismatchConstraint,
				ModelHelper.getElementsOfType(project.getModel(), sscaeProjectMD5ChecksumMismatchMetaclasses, true));
	}

	public volatile boolean hasPostEventNotifications = false;
	
	// latestProjectUsageGraph
	public volatile SSCAEProjectUsageGraph latestProjectUsageGraph;

	public boolean resolvedSSCAEProfileAndStereotypes() {
		if (null == sscaeProjectUsageIntegrityProfile) {
			sscaeProjectUsageIntegrityProfile = StereotypesHelper.getProfile(project, SSCAE_PROJECT_USAGE_INTEGRITY_PROFILE);
			if (null == sscaeProjectUsageIntegrityProfile)
				return false;
		}

		boolean resolved = (errorLevel != null);

		if (null == sscaeSharedPackageStereotype) {
			sscaeSharedPackageStereotype = StereotypesHelper.getStereotype(project, SSCAE_SHARED_PACKAGE_STEREOTYPE_NAME, sscaeProjectUsageIntegrityProfile);
			resolved &= (null != sscaeSharedPackageStereotype);
		}

		if (null == sscaeProjectModelStereotype) {
			sscaeProjectModelStereotype = StereotypesHelper.getStereotype(project, SSCAE_PROJECT_MODEL_STEREOTYPE_NAME, sscaeProjectUsageIntegrityProfile);
			resolved &= (null != sscaeProjectModelStereotype);
		}

		if (null == sscaeAbstractUsageStereotype) {
			sscaeAbstractUsageStereotype = StereotypesHelper.getStereotype(project, SSCAE_ABSTRACT_USAGE_STEREOTYPE_NAME, sscaeProjectUsageIntegrityProfile);
			resolved &= (null != sscaeAbstractUsageStereotype);
		}

		if (null == sscaeProjectUsageValidationSuite) {
			sscaeProjectUsageValidationSuite = (Package) ModelHelper.findInParent(
					sscaeProjectUsageIntegrityProfile, 
					SSCAE_PROJECT_USAGE_VALIDATION_SUITE_NAME, 
					Package.class);
			if (null == sscaeProjectUsageValidationSuite)
				return false;
		}

		if (null == sscaeValidProjectUsageGraphConstraint) {
			sscaeValidProjectUsageGraphConstraint = (Constraint) ModelHelper.findInParent(
					sscaeProjectUsageValidationSuite,
					SSCAE_VALID_PROJECT_USAGE_GRAPH_RULE_NAME,
					Constraint.class);
			if (null != sscaeValidProjectUsageGraphConstraint)
				validationSuiteConstraints.add(sscaeValidProjectUsageGraphConstraint);
			else
				resolved = false;
		}

		if (null == sscaeProjectStereotypeValidationConstraint) {
			sscaeProjectStereotypeValidationConstraint = (Constraint) ModelHelper.findInParent(
					sscaeProjectUsageValidationSuite,
					SSCAE_PROJECT_STEREOTYPE_VALIDATION_RULE_NAME,
					Constraint.class);
			if (null != sscaeProjectStereotypeValidationConstraint)
				validationSuiteConstraints.add(sscaeProjectStereotypeValidationConstraint);
			else
				resolved = false;
		}

		if (null == sscaeProjectMD5ChecksumMismatchConstraint) {
			sscaeProjectMD5ChecksumMismatchConstraint = (Constraint) ModelHelper.findInParent(
					sscaeProjectUsageValidationSuite,
					SSCAE_PROJECT_MD5_CHECKSUM_MISMATCH_RULE_NAME,
					Constraint.class);
			if (null != sscaeProjectMD5ChecksumMismatchConstraint)
				validationSuiteConstraints.add(sscaeProjectMD5ChecksumMismatchConstraint);
			else
				resolved = false;
		}

		if (null == sscaeProjectUsageRelationshipConstraint){
			sscaeProjectUsageRelationshipConstraint = (Constraint) ModelHelper.findInParent(
					sscaeProjectUsageValidationSuite,
					SSCAE_PROJECT_USAGE_RELATIONSHIP_RULE_NAME,
					Constraint.class);
			if (null != sscaeProjectUsageRelationshipConstraint)
				validationSuiteConstraints.add(sscaeProjectUsageRelationshipConstraint);
			else
				resolved = false;		
		}

		return resolved;
	}

	public Stereotype getAbstractUsageStereotype(){
		if (!resolvedSSCAEProfileAndStereotypes())
			return null;

		return sscaeAbstractUsageStereotype;
	}

	// SSCAEProjectModel

	public Stereotype getProjectModelStereotype(){
		if (!resolvedSSCAEProfileAndStereotypes())
			return null;
		return sscaeProjectModelStereotype;
	}

	public boolean hasSSCAEProjectModelStereotypeApplied(@Nonnull Model m) {
		if (!resolvedSSCAEProfileAndStereotypes())
			return false;

		return StereotypesHelper.hasStereotypeOrDerived(m, this.sscaeProjectModelStereotype);
	}

	public String getSSCAEProjectModelGraphSerialization(@Nonnull Model m) {
		if (!resolvedSSCAEProfileAndStereotypes())
			return "";

		List<String> values = StereotypesHelper.getStereotypePropertyValueAsString(m, this.sscaeProjectModelStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_SERIALIZATION);
		if (null == values || values.size() != 1)
			return "";

		return values.get(0);
	}

	public void setSSCAEProjectModelGraphSerialization(@Nonnull Model m, String serialization) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("No SSCAE ModelProject Stereotype");

		StereotypesHelper.setStereotypePropertyValue(m, this.sscaeProjectModelStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_SERIALIZATION, serialization);
	}

	public int getSSCAEProjectModelVersion(@Nonnull Model m) {
		if (!resolvedSSCAEProfileAndStereotypes())
			return 0;

		List<String> values = StereotypesHelper.getStereotypePropertyValueAsString(m, this.sscaeProjectModelStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_VERSION);
		if (null == values || values.size() != 1)
			return 0;

		try {
			return Integer.parseInt(values.get(0));
		} catch (NumberFormatException e) {
			logger.error(String.format("%s - getSSCAEProjectModelVersion(model='%s' {ID=%s}) cannot convert version info as integer: '%s'",
					ProjectUsageIntegrityPlugin.getInstance().getPluginName(),
					m.getName(), m.getID(),
					values.get(0)));
			return 0;
		}
	}

	public void setSSCAEProjectModelVersion(@Nonnull Model m, int versionNumber) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("No SSCAE ModelProject Stereotype");

		StereotypesHelper.setStereotypePropertyValue(m, this.sscaeProjectModelStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_VERSION, new Integer(versionNumber));
	}

	public void validateSSCAEProjectModelMD5(@Nonnull Model m, Set<Annotation> annotations) {
		Project mp = Project.getProject(m);
		if (null == mp || !(project.equals(mp)))
			return;

		if (!resolvedSSCAEProfileAndStereotypes())
			return;

		if (!project.getModel().equals(m))
			throw new IllegalArgumentException(
					String.format("validateSSCAEProjectModelMD5 -- project '%s' {ID=%s} with model {ID=%s} does not match model {ID=%s}",
							project.getName(), project.getModel().getID(), m.getID()));

		if (latestProjectUsageGraph == null)
			return;

		if (!hasSSCAEProjectModelStereotypeApplied(m))
			return;

		String expected = getSSCAEProjectModelGraphSerialization(m);
		String actual = latestProjectUsageGraph.getProjectUsageGraphSerialization();

		if (null == expected || expected.isEmpty() || expected.equals(actual))
			return;
		
		IPrimaryProject ip = mp.getPrimaryProject();
		if (ip instanceof TeamworkPrimaryProject) {
			
			if (Versioning.isLatestVersion(ip))
				return;
		}
		
		ComparisonFailure compare = new ComparisonFailure(
				String.format("MD5 checksum differences for SSCAE Local Project '%s' (expected = project's MD5; actual = computed MD5)", project.getName()),
				expected, actual);
		String mismatch = compare.getMessage();
		if (mismatch.length() > 100)
			mismatch = mismatch.substring(0, 100) + String.format(" ... (%d long!)", mismatch.length());
		
		EnumerationLiteral errorLevel = getValidationWarningLevel();
		
		Annotation a = new Annotation(errorLevel, "SSCAE MD5", mismatch, m);
		annotations.add(a);
	}

	public void validateSSCAERootModelProjectUsageRelationships(@Nonnull Model m, Set<Annotation> annotations) {
		Project mp = Project.getProject(m);
		if (null == mp || !(project.equals(mp)))
			return;

		if (!resolvedSSCAEProfileAndStereotypes())
			return;

		if (!project.getModel().equals(m))
			throw new IllegalArgumentException(
					String.format("validateSSCAEProjectModelMD5 -- project '%s' {ID=%s} with model {ID=%s} does not match model {ID=%s}",
							project.getName(), project.getModel().getID(), m.getID()));

		if (latestProjectUsageGraph == null)
			return;

		IPrimaryProject sourceP = project.getPrimaryProject();
		MDAbstractProject sourceV = latestProjectUsageGraph.vertexMap.get(sourceP);

		if (latestProjectUsageGraph.shouldBeSystemOrStandardProfile.contains(sourceV)) {
			annotations.add(createSSCAERepairSystemOrStandardProfileFlagAction(m, sourceP));
		}
		
		for (Map.Entry<IAttachedProject, SortedSet<Element>> entry : latestProjectUsageGraph.unloaded2proxiesMap.entrySet()) {
			IAttachedProject unloadedProject = entry.getKey();
			Set<Element> unloadedProxies = entry.getValue();
			Annotation a = createSSCAEUnloadedModuleProxyRepairAction(unloadedProject, unloadedProxies);
			if (null != a)
				annotations.add(a);
		}

		IDecompositionModel dm = (IDecompositionModel) sourceP.getService(IDecompositionModel.class);
		if (dm.isAvailable()) {
			IProjectUsageManager pum = (IProjectUsageManager) sourceP.getService(IProjectUsageManager.class);
			for (ProjectUsage usageRel : dm.getDecompositionProject().getProjectUsages()) {
				if (null != usageRel) {
					MDAbstractProjectUsage usageEdge = latestProjectUsageGraph.edgeMap.get(usageRel);

					IProject usedP = pum.getProject(usageRel);
					if (!(usedP instanceof IAttachedProject)) continue;
					IAttachedProject targetP = (IAttachedProject) usedP;

					MDAbstractProject targetV = latestProjectUsageGraph.vertexMap.get(targetP);

					if (null != usageEdge && null != targetV) {
						if (!usageEdge.isResolved())
							continue;

						if (!usageEdge.isReadOnly()) {
							annotations.add(createSSCAEReadOnlyUsageRepairAnnotation(m, sourceP, sourceV, usageEdge, targetV, targetP, usageRel));
						}

						if (!usageEdge.isReshared()) {
							annotations.add(createSSCAEReSharedUsageRepairAnnotation(m, sourceP, sourceV, usageEdge, targetV, targetP, usageRel));
						}

						if (!(usageEdge instanceof MDTeamworkProjectUsage))
							continue;

						MDTeamworkProjectUsage twUsageEdge = (MDTeamworkProjectUsage) usageEdge;
						if (!twUsageEdge.isSticky()) {
							annotations.add(createSSCAEStickyVersionUsageRepairAnnotation(m, sourceP, sourceV, usageEdge, targetV, targetP, usageRel));
						}
					}
				}
			}
		}

		for (Map.Entry<IAttachedProject, ProjectAttachmentConfiguration> missingEntry : latestProjectUsageGraph.missingDirectAttachments.entrySet()) {
			IAttachedProject missingP = missingEntry.getKey();
			ProjectAttachmentConfiguration config = missingEntry.getValue();
			annotations.add(createSSCAEMissingUsageRepairAnnotation(m, sourceP, missingP, config));
		}

		for (Map.Entry<Element, SortedSet<Element>> entry : latestProjectUsageGraph.owner2proxiesMap.entrySet()) {
			Element owner = entry.getKey();
			SortedSet<Element> proxies = entry.getValue();
			annotations.add(new Annotation(errorLevel, 
					String.format("SSCAE %s has nested proxies", owner.getHumanType()),
					String.format("'%s' : '%s' {ID=%s} has %d nested proxies", owner.getHumanName(), owner.getHumanType(), owner.getID(), proxies.size()),
					owner));		
		}


		for(Map.Entry<DiagramPresentationElement, SessionReport> entry : latestProjectUsageGraph.diagram2sessionReport.entrySet()) {
			DiagramPresentationElement dpe = entry.getKey();
			Diagram d = dpe.getDiagram();
			DiagramType dType = dpe.getDiagramType();

			SessionReport dpeReport = entry.getValue();
			if (dpeReport.numberOfViolations() == 0)
				continue;

			annotations.add(new Annotation(errorLevel, 
					String.format("SSCAE %s TraceContract Monitor Violation", dType.getType()),
					String.format("'%s' {ID=%s} has %d TraceContract Monitor Violations\n====\n%s\n=====",
							d.getQualifiedName(), d.getID(), dpeReport.numberOfViolations(), dpeReport.toString()),
							d));
		}		

		for(Map.Entry<DiagramPresentationElement, Set<Element>> entry : latestProjectUsageGraph.diagram2proxyUsages.entrySet()) {
			DiagramPresentationElement dpe = entry.getKey();
			Diagram d = dpe.getDiagram();
			DiagramType dType = dpe.getDiagramType();

			Set<Element> dpeProxies = entry.getValue();
			annotations.add(new Annotation(errorLevel, 
					String.format("SSCAE %s uses proxies", dType.getType()),
					String.format("'%s' {ID=%s} uses %d proxy elements",
							d.getQualifiedName(), d.getID(), dpeProxies.size()),
							d));
		}	
		
		for (MDAbstractProject lv : latestProjectUsageGraph.localModulesWithTeamworkIDs) {
			String classificationLabel = SSCAEProjectUsageGraph.ProjectClassificationShortLabel.get(lv.getClassification());
			String message = String.format("'%s' is a local %s with a teamwork ID '%s' located @ '%s'", lv.getName(), classificationLabel, lv.getProjectID(), lv.getLocation());
			String abbrev = String.format("SSCAE local %s has a teamwork ID", classificationLabel);
			if (lv instanceof MDLocalPrimaryProject) {
				annotations.add(new Annotation(warningLevel, abbrev, message, m));
			} else {
				List<Package> mountedPackages = getAttachedProjectMountedPackages(lv);
				for (Package pkg : mountedPackages) {
					annotations.add(new Annotation(warningLevel, abbrev, message, pkg)); 
				}
			}
		}
	}

	public void validateSSCAEOtherModelProjectUsageRelationships(@Nonnull Model m, Set<Annotation> annotations) {
		Project mp = Project.getProject(m);
		if (null == mp || !(project.equals(mp)))
			return;

		if (!resolvedSSCAEProfileAndStereotypes())
			return;

		if (latestProjectUsageGraph == null)
			return;

		if (!hasSSCAEProjectModelStereotypeApplied(m))
			return;

		if (!project.getModel().equals(m))
			throw new IllegalArgumentException(
					String.format("validateSSCAEProjectModelMD5 -- project '%s' {ID=%s} with model {ID=%s} does not match model {ID=%s}",
							project.getName(), project.getID(), project.getModel().getID(), m.getID()));

		annotations.add(createSSCAEStereotypeUnapplicationAnnotation(m, sscaeProjectUsageRelationshipConstraint, getProjectModelStereotype()));
	}

	// SSCAESharedPackage

	public Stereotype getSharedPackageStereotype(){
		resolvedSSCAEProfileAndStereotypes();
		return this.sscaeSharedPackageStereotype;
	}

	public boolean hasSSCAESharedPackageStereotypeApplied(@Nonnull Package p) {
		if (!resolvedSSCAEProfileAndStereotypes())
			return false;

		return StereotypesHelper.hasStereotypeOrDerived(p, this.sscaeSharedPackageStereotype);
	}

	public String getSSCAESharedPackageGraphSerialization(@Nonnull Package p) {
		if (!resolvedSSCAEProfileAndStereotypes())
			return "";

		List<String> values = StereotypesHelper.getStereotypePropertyValueAsString(p, this.sscaeSharedPackageStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_SERIALIZATION);
		if (null == values || values.size() != 1)
			return "";

		return values.get(0);
	}

	public void setSSCAESharedPackageGraphSerialization(@Nonnull Package p, String serialization) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("No SSCAE SharedProject Stereotype");

		StereotypesHelper.setStereotypePropertyValue(p, this.sscaeSharedPackageStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_SERIALIZATION, serialization);
	}

	public int getSSCAESharedPackageVersion(@Nonnull Package p) {
		if (!resolvedSSCAEProfileAndStereotypes())
			return 0;

		List<String> values = StereotypesHelper.getStereotypePropertyValueAsString(p, this.sscaeSharedPackageStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_VERSION);
		if (null == values || values.size() != 1)
			return 0;

		try {
			return Integer.parseInt(values.get(0));
		} catch (NumberFormatException e) {
			logger.error(String.format("%s - getSSCAESharedPackageVersion(shared package='%s' {ID=%s}) cannot convert version info as integer: '%s'",
					ProjectUsageIntegrityPlugin.getInstance().getPluginName(),
					p.getQualifiedName(), p.getID(),
					values.get(0)));
			return 0;
		}
	}

	public void setSSCAESharedPackageVersion(@Nonnull Package p, int versionNumber) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("No SSCAE SharedPackage Stereotype");

		StereotypesHelper.setStereotypePropertyValue(p, this.sscaeSharedPackageStereotype, ProjectUsageIntegrityHelper.SSCAE_PROJECT_STEREOTYPE_GRAPH_VERSION, new Integer(versionNumber));
	}

	protected boolean ranOnceMD5Validation = false;

	public boolean alreadyRanMD5Validation() {
		boolean ran = ranOnceMD5Validation;
		ranOnceMD5Validation = true;
		return ran;
	}

	public void validateSSCAESharedPackageMD5(@Nonnull Package p, Set<Annotation> annotations) {		
		Project pp = Project.getProject(p);
		if (null == pp || !(project.equals(pp)))
			return;

		if (!resolvedSSCAEProfileAndStereotypes())
			return;

		if (latestProjectUsageGraph == null)
			return;

		if (!hasSSCAESharedPackageStereotypeApplied(p))
			return;

		String expected = getSSCAESharedPackageGraphSerialization(p);
		String actual = latestProjectUsageGraph.getProjectUsageGraphSerialization();

		if (null == expected || expected.isEmpty() || expected.equals(actual))
			return;

		
		IProject ip = ProjectUtilities.getProject(p);
		if (ip instanceof ITeamworkProject) {
			
			if (Versioning.isLatestVersion(ip))
				return;
		}
		
		ComparisonFailure compare = new ComparisonFailure(
				String.format("MD5 checksum differences for SSCAE Shared Package '%s' (expected = project's MD5; actual = computed MD5)", p.getQualifiedName()),
				expected, actual);
		String mismatch = compare.getMessage();
		if (mismatch.length() > 100)
			mismatch = mismatch.substring(0, 100) + String.format(" ... (%d long!)", mismatch.length());
		
		EnumerationLiteral errorLevel = getValidationWarningLevel();
		
		Annotation a = new Annotation(errorLevel, "SSCAE MD5", mismatch, p);
		annotations.add(a);
	}

	// repair actions

	protected List<NMAction> createSSCAEActionList(@Nonnull NMAction action) {
		List<NMAction> actions = new ArrayList<NMAction>();
		if (null != applyAll) {
			actions.add(applyAll);
		}
		actions.add(action);
		return actions;
	}

	public Annotation createSSCAEReadOnlyUsageRepairAnnotation(@Nonnull Model m, 
			@Nonnull IPrimaryProject sourceP, @Nonnull MDAbstractProject u,
			@Nonnull MDAbstractProjectUsage u2v,
			@Nonnull MDAbstractProject v, @Nonnull IAttachedProject targetP, 
			@Nonnull ProjectUsage usageRelation) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAEReadOnlyUsageRepairAnnotation() -- SSCAE profile not yet resolved!");

		EnumerationLiteral errorLevel = getValidationErrorLevel();
		return new SSCAEAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				errorLevel,
				"SSCAE {read-only}",
				String.format("Set the usage relationship to {read-only} in:\n%s\n%s\n%s", u, v, u2v),
				m, createSSCAEActionList(new RepairProjectUsageToReadOnlyAction(this, m, sourceP, u, v, targetP)));
	}

	public Annotation createSSCAEReSharedUsageRepairAnnotation(@Nonnull Model m, 
			@Nonnull IPrimaryProject sourceP, @Nonnull MDAbstractProject u,
			@Nonnull MDAbstractProjectUsage u2v,
			@Nonnull MDAbstractProject v, @Nonnull IAttachedProject targetP, 
			@Nonnull ProjectUsage usageRelation) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAEReSharedUsageRepairAnnotation() -- SSCAE profile not yet resolved!");

		EnumerationLiteral errorLevel = getValidationErrorLevel();
		return new SSCAEAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				errorLevel, 
				"SSCAE {shared}",
				String.format("Set the usage relationship to {shared} in:\n%s\n%s\n%s", u, v, u2v),
				m, createSSCAEActionList(new RepairProjectUsageToReSharedAction(this, m, sourceP, u, v, targetP)));
	}

	public Annotation createSSCAEStickyVersionUsageRepairAnnotation(@Nonnull Model m, 
			@Nonnull IPrimaryProject sourceP, @Nonnull MDAbstractProject u,
			@Nonnull MDAbstractProjectUsage u2v,
			@Nonnull MDAbstractProject v, @Nonnull IAttachedProject targetP, 
			@Nonnull ProjectUsage usageRelation) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAEStickyVersionUsageRepairAnnotation() -- SSCAE profile not yet resolved!");

		EnumerationLiteral errorLevel = getValidationErrorLevel();
		return new SSCAEAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				errorLevel, 
				"SSCAE {sticky}",
				String.format("STICKY: Set the usage relationship to {sticky} in:\n%s\n%s\n%s", u, v, u2v),
				m, Collections.singletonList(new RepairProjectUsageToStickyVersionAction(this, m, sourceP, u, v, targetP)));
	}

	public Annotation createSSCAEMissingUsageRepairAnnotation(
			@Nonnull Model m,
			@Nonnull IPrimaryProject sourceP, 
			@Nonnull IAttachedProject targetP, 
			@Nonnull ProjectAttachmentConfiguration config) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAEMissingUsageRepairAnnotation() -- SSCAE profile not yet resolved!");

		EnumerationLiteral errorLevel = getValidationErrorLevel();
		return new SSCAEAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				errorLevel, 
				"SSCAE direct usage", 
				String.format("MISSING USAGE: Add a direct usage relationship for %s", targetP.getName()),
				m, createSSCAEActionList(new RepairAddMissingUsageRelationshipAction(this, m, sourceP, targetP, config)));
	}

	/**
	 * @param unloadedProject
	 * @param unloadedProxies
	 * @return may be null!
	 */
	public Annotation createSSCAEUnloadedModuleProxyRepairAction(
			@Nonnull IAttachedProject unloadedProject,
			@Nonnull Set<Element> unloadedProxies) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAERepairSystemOrStandardProfileFlagAction() -- SSCAE profile not yet resolved!");

		RepairUnloadedModuleWithProxiesAction action = new RepairUnloadedModuleWithProxiesAction(this, project, unloadedProject, unloadedProxies);
		Element context = action.getContext();
		if (null == context)
			return null;
		
		EnumerationLiteral errorLevel = getValidationErrorLevel();
		return new SSCAEUnloadedModuleAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				errorLevel, 
				action.getMessage(),
				context,
				createSSCAEActionList(action));
	}

	public Annotation createSSCAERepairSystemOrStandardProfileFlagAction(
			@Nonnull Model m,
			@Nonnull IPrimaryProject sourceP) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAERepairSystemOrStandardProfileFlagAction() -- SSCAE profile not yet resolved!");

		EnumerationLiteral errorLevel = getValidationErrorLevel();
		return new SSCAEAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				errorLevel, 
				"SSCAE Standard/System Profile Flag", 
				String.format("Set the 'Standard/System Profile' flag for this project"),
				m, createSSCAEActionList(new RepairSystemOrStandardProfileFlagAction(this, sourceP)));
	}
	
	public Annotation createSSCAEStereotypeApplicationAnnotation(@Nonnull Package p, @Nonnull Constraint c, @Nonnull Stereotype stereotype) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAEStereotypeApplicationAnnotation() -- SSCAE profile not yet resolved!");

		return new SSCAEAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				p, c, 
				String.format("Apply <<%s>> to '%s'",  
						stereotype.getName(),
						p.getQualifiedName()),
						createSSCAEActionList(new ApplyStereotypeAction(p, stereotype, this)));
	}

	public Annotation createSSCAEStereotypeUnapplicationAnnotation(@Nonnull Package p, @Nonnull Constraint c, @Nonnull Stereotype stereotype) {
		if (!resolvedSSCAEProfileAndStereotypes())
			throw new IllegalArgumentException("createSSCAEStereotypeUnapplicationAnnotation() -- SSCAE profile not yet resolved!");

		return new SSCAEAnnotation(
				latestProjectUsageGraph.getProjectUsageGraphSignature(),
				p, c, 
				String.format("Unapply <<%s>> to '%s'",  
						stereotype.getName(),
						p.getQualifiedName()),
						createSSCAEActionList(new UnapplyStereotypeAction(p, stereotype, this)));
	}

	protected ApplyAllSSCAERepairs applyAll;

	/**
	 * @see https://support.nomagic.com/browse/MDUMLCS-3042
	 */				
	public void runSSCAEValidationAndShowResults(final String sessionLabel) {
		
		if (!resolvedSSCAEProfileAndStereotypes())
			return;
		
		final Package suite = ProjectUsageIntegrityHelper.getSSCAEProjectUsageValidationSuite(project);
		if (suite == null)
			return;

		final Set<Element> elements = new HashSet<Element>();
		final List<RuleViolationResult> results = new ArrayList<RuleViolationResult>();

		applyAll = null; // new ApplyAllSSCAERepairs(project, this);

		try {
			for (Annotation a : runSSCAEValidProjectUsageGraphRule()) {
				elements.add((Element) a.getTarget());
				RuleViolationResult result = new RuleViolationResult(a, sscaeValidProjectUsageGraphConstraint);
				results.add(result);
			}

			for (Annotation a : runSSCAEProjectUsageRelationshipRule()) {
				elements.add((Element) a.getTarget());
				RuleViolationResult result = new RuleViolationResult(a, sscaeProjectUsageRelationshipConstraint);
				results.add(result);
			}

			for (Annotation a : runSSCAEProjectMD5ChecksumMismatchRule()) {
				elements.add((Element) a.getTarget());
				RuleViolationResult result = new RuleViolationResult(a, sscaeProjectMD5ChecksumMismatchConstraint);
				results.add(result);
			}

			for (Annotation a : runSSCAEProjectStereotypeValidationRule()) {
				elements.add((Element) a.getTarget());
				RuleViolationResult result = new RuleViolationResult(a, sscaeProjectStereotypeValidationConstraint);
				results.add(result);
			}
		} finally {
			applyAll = null;
		}

		final ValidationRunData runData = new ValidationRunData(suite, false, elements, errorLevel);
		Runnable r = new RunnableSessionWrapper(sessionLabel) {
			@Override
			public void run() {
				ValidationResultsWindowManager.updateValidationResultsWindow(
						"gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation", 
						"SSCAE ProjectUsage Validation Results", 
						runData, results);
			}
		};

		Utilities.invokeAndWaitOnDispatcher(r);
	}

	public ComputeProjectUsageGraphCommand runSSCAEValidationAndShowResultsIfCheckerEnabled(boolean showProjectUsageDiagnosticModalDialog) {
		return runSSCAEValidationAndShowResultsIfCheckerEnabled(false, true, SSCAEProjectUsageGraph.MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES, showProjectUsageDiagnosticModalDialog);
	}
	
	public @Nonnull ComputeProjectUsageGraphCommand runSSCAEValidationAndShowResultsIfCheckerEnabled(
			boolean anonymousVertexLabels,
			boolean includeLocalEdges,
			Set<String> excludedProjectNames, boolean showProjectUsageDiagnosticModalDialog) {
	
		ComputeProjectUsageGraphCommand c = new ComputeProjectUsageGraphCommand(project, 
				anonymousVertexLabels,
				includeLocalEdges,
				excludedProjectNames,
				showProjectUsageDiagnosticModalDialog);
		c.run();

		if (ProjectUsageIntegrityPlugin.getInstance().isProjectUsageIntegrityCheckerEnabled()) {
			// workaround
			// @see https://support.nomagic.com/browse/MDUMLCS-8884?focusedCommentId=50079#action_50079
			// @see https://support.nomagic.com/browse/MDUMLCS-2103
			EventSupport eventSupport = project.getRepository().getEventSupport();
			eventSupport.setEnableEventFiring(false);
			try {
				if (!c.isProjectUsageTopologyValid()) {
					runSSCAEValidationAndShowResults(String.format("SSCAE Validation('%s')", project.getName()));	
				}
			} finally {
				eventSupport.setEnableEventFiring(true);
			}
		}
		
		return c;
	}

	@Override
	public void notify(ProjectEvent ev) {
		ProjectEventType evType = ev.getEventType();
		logger.info(String.format("ProjectUsageIntegrity.notify(%s)", evType.name()));
		
		if (evType.isPostEvent()) {
			hasPostEventNotifications = true;
		}
		
		switch (evType) {
		
		case PRE_CLOSE: {
			if (this.pProject.getProjectListeners().contains(this))
				this.pProject.removeProjectListener(this);
			break;
		}
		
		case PRE_DETACH_PROJECT:
			if (ev instanceof DecompositionEvent) {
				DecompositionEvent dev = (DecompositionEvent) ev;
				IProject ap = dev.getAttachedProject();
				this.attachedProjectInfo.remove(ap);
			}
			break;
		
		case PRE_REMOVE_PROJECT:
			if (ev instanceof ProjectAttachOrDettachEvent) {
				ProjectAttachOrDettachEvent padev = (ProjectAttachOrDettachEvent) ev;
				IAttachedProject ap = padev.getAttachedOrDettachProject();
				this.attachedProjectInfo.remove(ap);
			}
			break;
		
		case PRE_ROLLBACK:
			if (ev instanceof ProjectAttachOrDettachEvent) {
				ProjectAttachOrDettachEvent padev = (ProjectAttachOrDettachEvent) ev;
				IAttachedProject ap = padev.getAttachedOrDettachProject();
				this.attachedProjectInfo.remove(ap);
			}
			break;
		
		case PRE_UNLOAD: 
			if (ev instanceof ProjectAttachOrDettachEvent) {
				ProjectAttachOrDettachEvent padev = (ProjectAttachOrDettachEvent) ev;
				IAttachedProject ap = padev.getAttachedOrDettachProject();
				this.attachedProjectInfo.remove(ap);
			}
			break;
		
		case PRE_REATTACH_PROJECT:
			if (ev instanceof DecompositionEvent) {
				DecompositionEvent dev = (DecompositionEvent) ev;
				IProject ap = dev.getAttachedProject();
				this.attachedProjectInfo.remove(ap);
			}
			break;
			
			
		case POST_REATTACH_PROJECT:
			if (ev instanceof DecompositionEvent) {
				DecompositionEvent dev = (DecompositionEvent) ev;
				IProject ap = dev.getAttachedProject();
				this.attachedProjectInfo.remove(ap);
			}
			break;
			
		case POST_ATTACH_PROJECT:
			if (ev instanceof DecompositionEvent) {
				checkPostAttachProject((DecompositionEvent)ev);
			}
			break;
			
			// workaround for MD's "unshared" usage relationships mess during export.
			// @see https://support.nomagic.com/browse/MDUMLCS-8884
		case POST_EXTRACT_PROJECT:
			if (ev instanceof ExtractProjectEvent) {
				ExtractProjectEvent addEvent = (ExtractProjectEvent) ev;
				IAttachedProject addedProject = addEvent.getExtractedProject();
				logger.info(String.format("ProjectUsageIntegrity.notify(POST_EXTRACT_PROJECT) - %s", addedProject.getName()));
				IDecompositionModel dm = (IDecompositionModel) addedProject.getService(IDecompositionModel.class);
				if (dm.isAvailable()) {
					IProjectUsageManager pum = (IProjectUsageManager) addedProject.getService(IProjectUsageManager.class);
					EList<ProjectUsage> usages = dm.getDecompositionProject().getProjectUsages();
					logger.info(String.format("ProjectUsageIntegrity.notify(POST_EXTRACT_PROJECT) - %s has %d usages", addedProject.getName(), usages.size()));
					for (ProjectUsage usageRel : usages) {
						IProject up = pum.getProject(usageRel);
						if (!usageRel.isReshared()) {
							logger.info(String.format("ProjectUsageIntegrity.notify(POST_EXTRACT_PROJECT) - Setting {reShared=true} for usage '%s' => '%s'", addedProject.getName(), up.getName()));
							usageRel.setReshared(true);
						}
					}
				}
			}
			break;
			
		default:
			break;
		}
	}
	
	protected void checkPostAttachProject(DecompositionEvent ev) {
		IProject attached = ev.getAttachedProject();
		
		checkIProjectResources(attached);
	}
	
	public boolean checkIProjectResources(IProject p) {
		Collection<Resource> resources = p.getManagedResources();
		StringBuffer buff = new StringBuffer();
		buff.append(String.format("checkIProjectResources: IProject '%s' {ID=%s} from %s with %d managed resources", p.getName(), p.getProjectID(), p.getLocationURI(), resources.size()));
		boolean allResourcesInProject = true;
		for (Resource r : resources) {
			EList<Diagnostic> errors = r.getErrors();
			EList<Diagnostic> warnings = r.getWarnings();
			URI rURI = r.getURI();
			EList<EObject> contents = r.getContents();
			
			IProject rProject = ProjectUtilities.getProject(r);
			allResourcesInProject = allResourcesInProject && (rProject != null);
			
			String rDescription = (rProject == null) ? "<null>" : rProject.getProjectID();
			buff.append(String.format("\n resource %s {project=%s, errors=%d, warnings=%d, eobjects=%d}", rURI, rDescription, errors.size(), warnings.size(), contents.size()));
			String query = rURI.query();
			if ("resource=com.nomagic.magicdraw.uml_model.shared_umodel".equals(query)) {
				for (EObject content : contents) {
					if (content instanceof Profile) {
						Profile sharedPrf = (Profile) content;
						buff.append(String.format("\n shared profile: %s {ID=%s}", sharedPrf.getQualifiedName(), sharedPrf.getID()));
					} else if (content instanceof Model) {
						Model sharedMdl = (Model) content;
						buff.append(String.format("\n shared model: %s {ID=%s}", sharedMdl.getQualifiedName(), sharedMdl.getID()));
					} else if (content instanceof Package) {
						Package sharedPkg = (Package) content;
						buff.append(String.format("\n shared package: %s {ID=%s}", sharedPkg.getQualifiedName(), sharedPkg.getID()));
					}
				}
			}
		}
		if (!allResourcesInProject) {
			String result = buff.toString();
			logger.info(result);
			System.out.println(result);
		}
		return allResourcesInProject;
	}
	
	public static String SSCAE_CLEAN_TEAMWORK_COMMIT_TAG_FORMAT = "SSCAE Clean Teamwork Commit {md5=%s}";
	
	public String computeTeamworkProjectSSCAECleanCommitTag() {
		Model m = project.getModel();
		if (!hasSSCAEProjectModelStereotypeApplied(m)) 
			return "WARNING: SSCAE Commit Tag Not Generated";
		
		String actual = getSSCAEProjectModelGraphSerialization(m);
		if (null == actual)
			throw new IllegalArgumentException("The project's model should have the SSCAE ProjectUsage Graph signature");
		
		String expected = latestProjectUsageGraph.getProjectUsageGraphSerialization();
		if (null == expected) 
			throw new IllegalArgumentException("The project's helper should have the SSCAE ProjectUsage Graph signature");
		
		if (!actual.equals(expected)) 
			throw new IllegalArgumentException(String.format("SSCAE ProjectUsage Graph signature mismatch\n== actual ==\n%s\n== expected ==\n%s\n======!", actual, expected));
			
		String signature = SSCAEProjectUsageGraph.getMD5FromString(actual);
		String sscaeCleanTag = String.format(SSCAE_CLEAN_TEAMWORK_COMMIT_TAG_FORMAT, signature);
		return sscaeCleanTag;
	}
	
	private WeakHashMap<IProject, MDAbstractProject> attachedProjectInfo = new WeakHashMap<IProject, MDAbstractProject>();
	private WeakHashMap<MDAbstractProject, List<Package>> attachedProjectMountedPackages = new WeakHashMap<MDAbstractProject, List<Package>>();
	
	public MDAbstractProject createVertexInternal(IProject p, SSCAEProjectUsageGraph pug, int index, int width) throws RemoteException {
		String idFormat = "%0" + width + "d";
		String idString = String.format(idFormat, index);

		if (p instanceof TeamworkPrimaryProject) {
			MDTeamworkPrimaryProject that = new MDTeamworkPrimaryProject();
			MDTeamworkPrimaryProject.configure(that, this.project, (ITeamworkProject) p, idString);
			that.setClassification(pug.projectClassification);
			return that;
		} 

		if (p instanceof LocalPrimaryProject) {
			MDLocalPrimaryProject that = new MDLocalPrimaryProject();
			MDLocalPrimaryProject.configure(that, this.project, (ILocalProjectInternal) p, idString);
			if (that.isInstallRootRelative() && !that.isLocalTemplate()) {
				boolean standardSystemProfile = ProjectUtilities.isStandardSystemProfile(p);
				if (!standardSystemProfile) {
					pug.shouldBeSystemOrStandardProfile.add(that);
				}
			}
			that.setClassification(pug.projectClassification);
			return that;
		}
		
		Collection<Resource> resources = p.getManagedResources();
		StringBuffer buff = new StringBuffer();
		buff.append(String.format("\n\ncreateVertex: IProject '%s' {ID=%s} from %s with %d managed resources", p.getName(), p.getProjectID(), p.getLocationURI(), resources.size()));
		
		if (!(p instanceof IAttachedProject)) {
			MDLog.getPluginsLog().fatal("Project should be an IAttachedProject: " + buff.toString());
			System.exit(-1);
		}
		
		if (attachedProjectInfo.containsKey(p)) {
			MDAbstractProject ap = attachedProjectInfo.get(p);
			if (ap instanceof MDAttachedProject) {
				MDAttachedProject atp = (MDAttachedProject) ap;
				String pid = p.getProjectID();
				if (ap.getProjectID().equals(pid)) {
					atp.updateIndex(idString);
					return ap;
				}
				throw new RuntimeException("Project ID mismatch");
			}
		}
		
		List<Package> mountedPackages = new ArrayList<Package>();
		
		boolean allResourcesInProject = true;
		for (Resource r : resources) {
			EList<Diagnostic> errors = r.getErrors();
			EList<Diagnostic> warnings = r.getWarnings();
			URI rURI = r.getURI();
			EList<EObject> contents = r.getContents();

			IProject rProject = ProjectUtilities.getProject(r);
			allResourcesInProject = allResourcesInProject && (p.equals(rProject));

			String rDescription = (rProject == null) ? "<null>" : rProject.getProjectID();
			buff.append(String.format("\n resource %s {project=%s, errors=%d, warnings=%d, eobjects=%d}", rURI, rDescription, errors.size(), warnings.size(), contents.size()));

			String query = rURI.query();
			if ("resource=com.nomagic.magicdraw.uml_umodel.shared_umodel".equals(query)) {
				for (EObject content : contents) {
					Package pkg = null;
					if (content instanceof Profile) {
						pkg = (Profile) content;
						buff.append(String.format("\n shared profile: %s {ID=%s}", pkg.getQualifiedName(), pkg.getID()));
					} else if (content instanceof Model) {
						pkg = (Model) content;
						buff.append(String.format("\n shared model: %s {ID=%s}", pkg.getQualifiedName(), pkg.getID()));
					} else if (content instanceof Package) {
						pkg = (Package) content;
						buff.append(String.format("\n shared package: %s {ID=%s}", pkg.getQualifiedName(), pkg.getID()));
					}
					if (!pug.managedSharedPackages.contains(pkg)) {
						mountedPackages.add(pkg);
					}
				}
			}
		}
				
		MDAbstractProject ap = null;
		MDAttachedProject att = null;
		
		if (p instanceof TeamworkAttachedProject) {
			MDTeamworkAttachedProject that = new MDTeamworkAttachedProject();
			MDTeamworkAttachedProject.configure(that, this.project, (ITeamworkProject) p, idString);
			ap = that;
			att = that;
		} else if (p instanceof LocalAttachedProject) {
			MDLocalAttachedProject that = new MDLocalAttachedProject();
			MDLocalAttachedProject.configure(that, this.project, (ILocalProjectInternal) p, idString);
			if (that.isInstallRootRelative() && !that.isLocalTemplate()) {
				boolean standardSystemProfile = ProjectUtilities.isStandardSystemProfile(p);
				if (!standardSystemProfile) {
					pug.shouldBeSystemOrStandardProfile.add(that);
				}
			}
			ap = that;
			att = that;
		}

		if (!mountedPackages.isEmpty()) {
			String serialization = this.getSSCAESharedPackageGraphSerialization(mountedPackages.get(0));
			if (null != serialization && (serialization.startsWith("--- !SSCAE.Digest") || serialization.startsWith("!SSCAE.Digest"))) {
				try {
					SSCAEProjectDigest digest = YamlDigestHelper.load(serialization);
					if (null != digest) {
						ProjectClassification pc = digest.getClassification();
						if (pc != null) {
							ap.setClassification(pc);
							buff.append(String.format("\n***** => classification = %s", SSCAEProjectUsageGraph.ProjectClassificationShortLabel.get(pc)));
						}
						att.setDigest(digest);
					}
				} catch (YAMLException ye) {
					ye.fillInStackTrace();
					ye.printStackTrace(System.out);	
				}
			}
			
			if (ApplicationEnvironment.isDeveloper()) {
				String result = buff.toString();
				MDLog.getPluginsLog().info(result);
			}
		}
		
		attachedProjectInfo.put(p, ap);
		attachedProjectMountedPackages.put(ap, mountedPackages);
		
		return ap;
	}

	public List<Package> getAttachedProjectMountedPackages(MDAbstractProject ap) {
		List<Package> mPkgs = attachedProjectMountedPackages.get(ap);
		if (null == mPkgs)
			return Collections.emptyList();
		return mPkgs;
	}
}
