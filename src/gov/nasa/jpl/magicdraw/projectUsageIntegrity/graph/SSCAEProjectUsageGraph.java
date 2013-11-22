package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;
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

import gov.nasa.jpl.logfire.Monitor;
import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.logfire.SessionReport;
import gov.nasa.jpl.magicdraw.appenders.Appender;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.options.SSCAEProjectUsageIntegrityOptions;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEProfileValidation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;
import javax.imageio.IIOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.MaskFunctor;
import org.jgrapht.graph.MaskSubgraph;

import com.nomagic.ci.metamodel.project.MountPoint;
import com.nomagic.ci.metamodel.project.ProjectUsage;
import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.local.decomposition.IProjectDecompositionManager;
import com.nomagic.ci.persistence.decomposition.ProjectAttachmentConfiguration;
import com.nomagic.ci.persistence.local.spi.AbstractAttachedProject;
import com.nomagic.ci.persistence.local.spi.AbstractProject;
import com.nomagic.ci.persistence.local.spi.decomposition.IDecompositionModel;
import com.nomagic.ci.persistence.local.spi.decomposition.IProjectUsageManager;
import com.nomagic.ci.persistence.local.spi.localproject.LocalAttachedProject;
import com.nomagic.ci.persistence.versioning.IVersionDescriptor;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.ProjectUtilitiesInternal;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.proxy.ProxyManager;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.magicdraw.teamwork.application.storage.ITeamworkProject;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkAttachedProject;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkPrimaryProject;
import com.nomagic.magicdraw.ui.EnvironmentLockManager;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.PresentationElement;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

/**
 * @author Nicolas F. Rouquette (JPL)
 * @author Alek Kerzhner (JPL)
 * @contributor Vygantas Gedgaudas (NoMagic)
 * @contributor Donatas Simkunas (NoMagic)
 * @contributor Martynas Lelevicius (NoMagic)
 * @see https://support.nomagic.com/browse/MDUMLCS-8816
 */
public class SSCAEProjectUsageGraph {

	public static MessageDigest md5;

	static {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			md5 = null;
		}
	}

	/**
	 * There are circularities in MD's local profiles/libraries.
	 * This is the list of their project names (not file names)
	 * See: https://support.nomagic.com/browse/SYSMLCS-302
	 * 
	 * Adjust the scope of the MD circular local profiles & libraries as needed...
	 */
	public static Set<String> MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES = new HashSet<String>();

	static {
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("BPMN2 Customization");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("MD_customization_for_SysML");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("Matrix_Templates_Profile");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("QUDV");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("SSCAEProjectUsageIntegrityProfile");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("SI ValueType Library");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("SIDefinitions");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("SISpecializations");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("SysML Profile");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("SysML constraints");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("SysML_matrix_templates_module");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("Traceability Customization");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("UML correctness constraints");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("UML completeness constraints");
		MD_CIRCULAR_LOCAL_PROFILES_AND_LIBRARIES.add("UML_Standard_Profile");
	}

	public static Set<String> MD_EXCLUDE_URI_CHECK_PROFILES_AND_PACKAGES = new HashSet<String>();
	
	static {
		MD_EXCLUDE_URI_CHECK_PROFILES_AND_PACKAGES.add("UML Standard Profile::UML2 Metamodel");
		MD_EXCLUDE_URI_CHECK_PROFILES_AND_PACKAGES.add("UML Standard Profile::StandardProfileL3");
		MD_EXCLUDE_URI_CHECK_PROFILES_AND_PACKAGES.add("UML Standard Profile::StandardProfileL2");

	}

	public SSCAEProjectUsageGraph(
			@Nonnull Project p,
			boolean anonymousVertexLabels,
			boolean includeLocalEdges,
			Set<String> excludedProjectNames) throws RemoteException {
		this.project = p;
		this.plugin = ProjectUsageIntegrityPlugin.getInstance();
		this.pluginName = plugin.getPluginName();
		this.appender = plugin.getLogTraceContractsAppender();
		this.helper = plugin.getSSCAEProjectUsageIntegrityProfileForProject(p);
		this.projectClassification = this.helper.getProjectClassification();
		this.useAnonymousVertexLabels = anonymousVertexLabels;
		this.includeLocalEdges = includeLocalEdges;
		this.excludedProjectNames = ((null == excludedProjectNames) ? Collections.<String>emptySet() : Collections.unmodifiableSet(excludedProjectNames));
		
		this.primaryProject = p.getPrimaryProject();
		Collection<Resource> projectManagedResources = this.primaryProject.getManagedResources();
		
		Stereotype S = helper.getSharedPackageStereotype();
		List<Element> sharedElementPackages = StereotypesHelper.getExtendedElements(S);
		
		List<Package> _managedSharedPackages = new ArrayList<Package>();
		for (Element e : sharedElementPackages) {
			Package pkg = (Package) e;
			Resource pkgR = pkg.eResource();
			if (projectManagedResources.contains(pkgR))
				_managedSharedPackages.add((Package) e);
		}
		this.managedSharedPackages = Collections.unmodifiableList(_managedSharedPackages);
		
		
		final IProjectUsageManager projectUsageManager = this.primaryProject.getService(IProjectUsageManager.class);
		List<ProjectUsage> brokenUsages = new ArrayList<ProjectUsage>();
		for (ProjectUsage projectUsage : this.primaryProject.getService(IDecompositionModel.class).getDecompositionProject().getProjectUsages()) {
			if (projectUsage.isReshared()) {
				if (projectUsageManager.getProject(projectUsage)==null)
					brokenUsages.add(projectUsage);
			}
		}
		
		if (brokenUsages.isEmpty()) {
			this.projectUsageInfo = "OK";
		} else {
			StringBuffer buff = new StringBuffer();
			int brokenCount = brokenUsages.size();
			buff.append(String.format("Found %d ProjectUsages without projects in %s %s\n", 
					brokenCount,
					this.projectClassification,
					this.primaryProject.getLocationURI()));
			int i = 1;
			for (ProjectUsage brokenUsage : brokenUsages) {
				Resource r = brokenUsage.eResource();
				EList<MountPoint> mountPoints = brokenUsage.getMountPoints();
				String rURI = (null == r) ? "<no resource URI>" : r.getURI().toString();
				buff.append(String.format("\n[%d/%d] no project for %s usage in %s\n- via %s mount points of: %s", 
						i++, brokenCount,
						(brokenUsage.eIsProxy() ? "proxy" : "non-proxy"),
						rURI,
						mountPoints.size(),
						brokenUsage.getUsedProjectURI()));
				for (MountPoint mp : mountPoints) {
					EObject mountedOn = mp.getMountedOn();
					URI mountedOnURI = (mountedOn == null) ? null : EcoreUtil.getURI(mountedOn);
					buff.append(String.format("\n--  mounted on: %s", (mountedOnURI == null) ? "N/A" : mountedOnURI));
					EObject mountPoint = mp.getMountedPoint();
					URI mountPointURI = (mountPoint == null) ? null : EcoreUtil.getURI(mountPoint);
					buff.append(String.format("\n-- mount point: %s", (mountPointURI == null) ? "<N/A>" : mountPointURI));					
				}
			}
			
			this.projectUsageInfo = buff.toString();
		} 

		if (projectClassification == ProjectClassification.INVALID) {
			return;
		}
		
		createGraph();
	}
	
	public final Project project;
	protected final IPrimaryProject primaryProject;
	protected final String pluginName;
	protected final Appender appender;
	protected final ProjectUsageIntegrityHelper helper;
	protected final Logger pluginLog = MDLog.getPluginsLog();
	protected final ProjectUsageIntegrityPlugin plugin;
	public final List<Package> managedSharedPackages;
	
	
	/**
	 * false = the names of the projects will be included in the DOT graph
	 * true = only the canonical index of the project appears in the DOT graph (i.e., L<N> or T<N>)
	 */
	public final boolean useAnonymousVertexLabels;

	/**
	 * false = the DOT graph is restricted to ProjectUsage relationships for teamwork projects only
	 * true = the DOT graph includes all ProjectUsage relationships for teamwork and local projects
	 */
	public final boolean includeLocalEdges;

	/**
	 * The set of project names to exclude from the ProjectUsage graph
	 */
	public final Set<String> excludedProjectNames;

	/**
	 * read-only
	 */
	public final ListenableDirectedMultigraph<MDAbstractProject, MDAbstractProjectUsage> projectUsageDirectedMultigraph = new ListenableDirectedMultigraph<MDAbstractProject, MDAbstractProjectUsage>(MDAbstractProjectUsage.class);

	protected final StringBuffer gSignature = new StringBuffer();
	public String getProjectUsageGraphSignature() { return gSignature.toString(); }

	protected final StringBuffer gMessages = new StringBuffer();
	public String getProjectUsageGraphMessages() { return gMessages.toString(); }

	protected final StringBuffer gSerialization = new StringBuffer();
	public String getProjectUsageGraphSerialization() { return gSerialization.toString(); }

	protected final StringBuffer gDiagnostic = new StringBuffer();
	public String getProjectUsageGraphDiagnostic() { return gDiagnostic.toString(); }

	protected final StringWriter gDOT = new StringWriter();
	public String getProjectUsageGraphDOTExport() { return gDOT.toString(); }

	protected final StringWriter gDOTteamworkOnly = new StringWriter();
	public String getProjectTeamworkUsageGraphDOTExport() { return gDOTteamworkOnly.toString(); }

	protected int proxyCount = 0;
	protected int proxyUnloadedOkCount = 0;
	public final Set<Element> proxyUnloadedOtherCount = new HashSet<Element>();
	protected int proxyGhostOkCount = 0;
	public final Set<Element> proxyGhostOtherCount = new HashSet<Element>();
	protected int diagramCount = 0;

	/**
	 * read-only
	 */
	public final SortedMap<Element, SortedSet<Element>> owner2proxiesMap = new TreeMap<Element, SortedSet<Element>>();
	public final Map<IAttachedProject, SortedSet<Element>> unloaded2proxiesMap = new HashMap<IAttachedProject, SortedSet<Element>>();

	/**
	 * read-only
	 */
	public final SortedMap<DiagramPresentationElement, Set<Element>> diagram2proxyUsages = new TreeMap<DiagramPresentationElement, Set<Element>>();


	/**
	 * read-only
	 */
	public final Map<DiagramPresentationElement, SessionReport> diagram2sessionReport = new HashMap<DiagramPresentationElement, SessionReport>();

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProject> localModulesWithTeamworkIDs = new TreeSet<MDAbstractProject>(PROJECT_VERTEX_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProject> moduleWithMissingShares = new TreeSet<MDAbstractProject>(PROJECT_VERTEX_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProject> missingProjects = new TreeSet<MDAbstractProject>(PROJECT_VERTEX_COMPARATOR);

	/**
	 * read-only
	 */
	public final Map<IProject, MDAbstractProject> vertexMap = new HashMap<IProject, MDAbstractProject>();

	/**
	 * read-only
	 */	
	public final Map<ProjectUsage, MDAbstractProjectUsage> edgeMap = new HashMap<ProjectUsage, MDAbstractProjectUsage>();

	/**
	 * read-only
	 */	
	public final Map<MDAbstractProject, List<MDAbstractProjectUsage>> vertexUsageEdges = new HashMap<MDAbstractProject, List<MDAbstractProjectUsage>>();

	/**
	 * read-only
	 */
	public final Map<MDAbstractProject, List<MDAbstractProjectUsage>> vertexUsedByEdges = new HashMap<MDAbstractProject, List<MDAbstractProjectUsage>>();

	/**
	 * read-only
	 */
	public final Map<MDAbstractProject, String> vertexUsageConsistencyLabel = new HashMap<MDAbstractProject, String>();

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProject> stronglyConnectedVertices = new TreeSet<MDAbstractProject>(PROJECT_VERTEX_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProjectUsage> stronglyConnectedEdges = new TreeSet<MDAbstractProjectUsage>(PROJECT_USAGE_EDGE_COMPARATOR);
	public final SortedSet<MDAbstractProjectUsage> unresolvedUsageEdges = new TreeSet<MDAbstractProjectUsage>(PROJECT_USAGE_EDGE_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProjectUsage> inconsistentUsageEdges = new TreeSet<MDAbstractProjectUsage>(PROJECT_USAGE_EDGE_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProject> inconsistentlyUsedVertices = new TreeSet<MDAbstractProject>(PROJECT_VERTEX_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProject> shouldBeSystemOrStandardProfile = new TreeSet<MDAbstractProject>(PROJECT_VERTEX_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedSet<MDAbstractProjectUsage> invalidUsageEdges = new TreeSet<MDAbstractProjectUsage>(PROJECT_USAGE_EDGE_COMPARATOR);

	/**
	 * read-only
	 */
	public final SortedMap<IAttachedProject, ProjectAttachmentConfiguration> missingDirectAttachments = new TreeMap<IAttachedProject, ProjectAttachmentConfiguration>(IPROJECT_COMPARATOR);

	/**
	 *  read-only
	 */
	public final SortedMap<Profile, Profile> nonUniqueNamesProfiles = new TreeMap<Profile, Profile>();
	
	/**
	 *  read-only
	 */
	public final SortedMap<Profile, Profile> nonUniqueURIProfiles = new TreeMap<Profile, Profile>();
	
	/**
	 *  read-only
	 */
	public final SortedMap<Package, Package> nonUniqueURIPackages = new TreeMap<Package, Package>();
	
	
	public boolean isProjectMissingSystemOrStandardProfileFlag() {
		return shouldBeSystemOrStandardProfile.contains(vertexMap.get(primaryProject));
	}

	public boolean isProjectUsageTopologyValid() { 
		return projectClassification != ProjectClassification.INVALID 
				&& missingProjects.isEmpty()
				&& moduleWithMissingShares.isEmpty()
				&& stronglyConnectedVertices.isEmpty() 
				&& stronglyConnectedEdges.isEmpty() 
				&& inconsistentUsageEdges.isEmpty()
				&& inconsistentlyUsedVertices.isEmpty()
				&& shouldBeSystemOrStandardProfile.isEmpty()
				&& invalidUsageEdges.isEmpty()
				&& proxyCount == 0
				&& owner2proxiesMap.isEmpty()
				&& unloaded2proxiesMap.isEmpty()
				&& diagram2sessionReport.isEmpty()
				&& diagram2proxyUsages.isEmpty()
				&& !isProjectMissingSystemOrStandardProfileFlag()
				&& missingDirectAttachments.isEmpty()
				&& nonUniqueNamesProfiles.isEmpty()
				&& nonUniqueURIProfiles.isEmpty()
				&& nonUniqueURIPackages.isEmpty();
	}

	protected boolean isTemplate;

	public boolean isLocalTemplate() { return this.isTemplate; }

	public final ProjectClassification projectClassification;
	public final String projectUsageInfo;
	
	public String getProjectClassificationLabel() { 
		if (null == projectClassification)
			return "";
				
		return ProjectClassificationLabel.get(projectClassification);
	}
	
	public static Map<ProjectClassification, String> ProjectClassificationLabel;
	public static Map<ProjectClassification, String> ProjectClassificationShortLabel;
	
	static {
		ProjectClassificationLabel = new HashMap<ProjectClassification, String>();
		ProjectClassificationLabel.put(ProjectClassification.INVALID, 
				"(not available, invalid project usage data)");
		ProjectClassificationLabel.put(ProjectClassification.IS_PROJECT, 
				"project (private data; nothing shared)");
		ProjectClassificationLabel.put(ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_RECOVERED_ELEMENTS, 
				"project (private data; nothing shared) - recovered elements!");
		ProjectClassificationLabel.put(ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_ELEMENTS,
				"project (private data; nothing shared) - missing elements!");
		ProjectClassificationLabel.put(ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS,
				"project (private data; nothing shared) - recovered and missing elements!");
		ProjectClassificationLabel.put(ProjectClassification.IS_MODULE, 
				"module (no private data; shared packages)");
		ProjectClassificationLabel.put(ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS,
				"module (no private data; shared packages) - recovered elements!");				
		ProjectClassificationLabel.put(ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS,
				"module (no private data; shared packages) - missing elements!");				
		ProjectClassificationLabel.put(ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS,
				"module (no private data; shared packages) - recovered and missing elements!");					
		ProjectClassificationLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE,
				"project/module hybrid (private data; shared packages)");
		ProjectClassificationLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS,
				"project/module hybrid (private data; shared packages) - recovered elements!");				
		ProjectClassificationLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS,
				"project/module hybrid (private data; shared packages) - missing elements!");				
		ProjectClassificationLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS,
				"project/module hybrid (private data; shared packages) - recovered and missing elements!");				
		
		ProjectClassificationShortLabel = new HashMap<ProjectClassification, String>();
		ProjectClassificationShortLabel.put(ProjectClassification.INVALID, 
				"INVALID");
		ProjectClassificationShortLabel.put(ProjectClassification.IS_PROJECT, 
				"project");
		ProjectClassificationShortLabel.put(ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_RECOVERED_ELEMENTS, 
				"project [recovered elements]");
		ProjectClassificationShortLabel.put(ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_ELEMENTS,
				"project [missing elements]");
		ProjectClassificationShortLabel.put(ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS,
				"project [recovered + missing elements]");
		ProjectClassificationShortLabel.put(ProjectClassification.IS_MODULE, 
				"module");
		ProjectClassificationShortLabel.put(ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS,
				"module [recovered elements]");				
		ProjectClassificationShortLabel.put(ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS,
				"module [missing elements]");				
		ProjectClassificationShortLabel.put(ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS,
				"module [recovered + missing elements]");					
		ProjectClassificationShortLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE,
				"hybrid");
		ProjectClassificationShortLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS,
				"hybrid [recovered elements]");				
		ProjectClassificationShortLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS,
				"hybrid [missing elements]");				
		ProjectClassificationShortLabel.put(ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS,
				"hybrid [recovered + missing elements]");				
	}


	protected void createGraph() throws RemoteException {	
		final List<IProject> allSortedProjects = new ArrayList<IProject>();
		allSortedProjects.addAll(ProjectUtilities.getAllProjects(this.project));
		Collections.sort(allSortedProjects, IPROJECT_COMPARATOR);
		boolean performanceLoggingEnabled = ProjectUsageIntegrityPlugin.getInstance().isPerformanceLoggingEnabled();
		int width = Integer.toString(allSortedProjects.size()).length();
	
		Long currentTime;
		Long previousTime = System.currentTimeMillis();
				
		MDAbstractProject vPrimary = createVertex(this.primaryProject, width);
		this.isTemplate = vPrimary.isLocalTemplate();
		for (IProject aProject : allSortedProjects) {
			createVertex(aProject, width);
		}
		
		
		if (performanceLoggingEnabled){ 
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Create Graph: " + (currentTime-previousTime) + " (ms)"); 
			previousTime = System.currentTimeMillis();
		};
		
		
		for (IProject aProject : allSortedProjects) {
			MDAbstractProject p1 = lookupVertex(aProject);
			List<MDAbstractProjectUsage> p1Usages = new ArrayList<MDAbstractProjectUsage>();

			AbstractProject ap = (AbstractProject) aProject;
			if (ap.isProjectAvailable() && ! this.excludedProjectNames.contains(aProject.getName())) {
				IDecompositionModel dm = (IDecompositionModel) ap.getService(IDecompositionModel.class);
				if (dm.isAvailable()) {
					IProjectUsageManager pum = (IProjectUsageManager) ap.getService(IProjectUsageManager.class);
					for (ProjectUsage pu : dm.getDecompositionProject().getProjectUsages()) {
						if (pu != null) {
							IProject up = pum.getProject(pu);
							if (up instanceof AbstractAttachedProject) {
								AbstractAttachedProject aap = (AbstractAttachedProject) up;
								MDAbstractProject p2 = lookupVertex(aap);
								MDAbstractProjectUsage e = null;
								if (aap instanceof TeamworkAttachedProject) {
									TeamworkAttachedProject tap = (TeamworkAttachedProject) aap;
									e = createTeamworkEdge(p1, p2, pu, tap);
								} else if (aap instanceof LocalAttachedProject) {
									LocalAttachedProject lap = (LocalAttachedProject) aap;
									e = createLocalEdge(p1, p2, pu, lap);
								} else
									throw new IllegalArgumentException("unhandled AbstractAttachedProject: " + aap.getClass().getName());
								p1Usages.add(e);
								createEdge(e);
								vertexUsedByEdges.get(p2).add(e);
								if (!e.isResolved())
									unresolvedUsageEdges.add(e);
							}
						}
					}
				}
			}
			
			Collections.sort(p1Usages, PROJECT_USAGE_EDGE_COMPARATOR);
			vertexUsageEdges.put(p1, p1Usages);
	
		}
		
		
		if (performanceLoggingEnabled){  
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Assign type of nodes: " + (currentTime-previousTime) + " (ms)"); 
			previousTime = System.currentTimeMillis();
		};
		
		StrongConnectivityInspector<MDAbstractProject, MDAbstractProjectUsage> detector =
				new StrongConnectivityInspector<MDAbstractProject, MDAbstractProjectUsage>(projectUsageDirectedMultigraph);
		List<DirectedSubgraph<MDAbstractProject, MDAbstractProjectUsage>> stronglyConnectedSubgraphs = detector.stronglyConnectedSubgraphs();

		
		if (performanceLoggingEnabled){ 
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check circular dependencies: " + (currentTime-previousTime) + " (ms)");
			previousTime = System.currentTimeMillis();
		};
		
		
		for (DirectedSubgraph<MDAbstractProject, MDAbstractProjectUsage> stronglyConnectedSubgraph : stronglyConnectedSubgraphs) {
			Set<MDAbstractProject> vertexSubset = stronglyConnectedSubgraph.vertexSet();
			Set<MDAbstractProjectUsage> edgeSubset = stronglyConnectedSubgraph.edgeSet();
			if (edgeSubset.isEmpty()) continue;

			for (MDAbstractProject v : vertexSubset) stronglyConnectedVertices.add(v);
			for (MDAbstractProjectUsage e : edgeSubset) stronglyConnectedEdges.add(e);
		}
		
		if (performanceLoggingEnabled){  
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Record circular dependencies: " + (currentTime-previousTime) + " (ms)");
			previousTime = System.currentTimeMillis();
		};
		
		for (IProject aProject : allSortedProjects) {
			MDAbstractProject p2 = lookupVertex(aProject);
			if (p2.isProjectMissing())
				missingProjects.add(p2);

			List<MDAbstractProjectUsage> p2UsedBy = vertexUsedByEdges.get(p2);
			Collections.sort(p2UsedBy, PROJECT_USAGE_EDGE_COMPARATOR);

			Map<String, List<MDAbstractProjectUsage>> signature2UsedBy = new HashMap<String, List<MDAbstractProjectUsage>>();
			for (MDAbstractProjectUsage p12 : p2UsedBy) {
				String signature = p12.getSignature();
				if (!signature2UsedBy.containsKey(signature))
					signature2UsedBy.put(signature, new ArrayList<MDAbstractProjectUsage>());
				signature2UsedBy.get(signature).add(p12);
			}

			Set<String> distinctSignatures = signature2UsedBy.keySet();
			int count = distinctSignatures.size();

			if (count == 0) continue;

			if (count == 1) {
				String sig = distinctSignatures.iterator().next();
				for (MDAbstractProjectUsage usage : signature2UsedBy.get(sig)) {
					String label = "OK";
					if (!usage.isValidUsage()) {
						invalidUsageEdges.add(usage);
						label = "INVALID";
						if (stronglyConnectedEdges.contains(usage))
							label += ", CYCLIC";
					} else {
						if (stronglyConnectedEdges.contains(usage))
							label = "CYCLIC";
					}
					vertexUsageConsistencyLabel.put(p2, distinctSignatures.iterator().next());
					usage.setUsageConsistencyLabel(label);
				}
			} else {
				int c = 0;
				for (String sig : distinctSignatures) {
					String label = String.format("INCONSISTENT[%d/%d]", ++c, count);
					for (MDAbstractProjectUsage usage : signature2UsedBy.get(sig)) {
						String ulabel = label;
						if (stronglyConnectedEdges.contains(usage))
							ulabel += ", CYCLIC";
						if (!usage.isValidUsage()) {
							ulabel += ", INVALID";
							invalidUsageEdges.add(usage);
						}
						inconsistentUsageEdges.add(usage);
						inconsistentlyUsedVertices.add(usage.getTarget());
						usage.setUsageConsistencyLabel(ulabel);
					}
				}
			}
		}
		
		
		if (performanceLoggingEnabled){  
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check consistency of usages: " + (currentTime-previousTime) + " (ms)"); 
			previousTime = System.currentTimeMillis();
		};
		

		IPrimaryProject sourceP = this.primaryProject;
		Set<IAttachedProject> missingDirectProjectUsages = new HashSet<IAttachedProject>();
		missingDirectProjectUsages.addAll(ProjectUtilities.getAllAttachedProjects(sourceP));
		IDecompositionModel dm = (IDecompositionModel) sourceP.getService(IDecompositionModel.class);
		if (dm.isAvailable()) {
			IProjectUsageManager pum = (IProjectUsageManager) sourceP.getService(IProjectUsageManager.class);
			for (ProjectUsage usageRel : dm.getDecompositionProject().getProjectUsages()) {
				if (null != usageRel) {
					IProject usedP = pum.getProject(usageRel);
					if (!(usedP instanceof IAttachedProject)) continue;
					IAttachedProject targetP = (IAttachedProject) usedP;
					missingDirectProjectUsages.remove(targetP);
				}
			}
		}
		

		if (performanceLoggingEnabled){  
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check missing usages: " + (currentTime-previousTime) + " (ms)");
			previousTime = System.currentTimeMillis();
		};
		

		if (!missingDirectProjectUsages.isEmpty()) {
			MDAbstractProject u = lookupVertex(sourceP);
			for (IAttachedProject missingP : missingDirectProjectUsages) {
				if (vertexMap.containsKey(missingP)) {
					MDAbstractProject v = vertexMap.get(missingP);
					if (!inconsistentlyUsedVertices.contains(v)) {
						IProjectDecompositionManager missingDM = ProjectUtilitiesInternal.getDecompositionManager(missingP);
						Map<IProject, ProjectAttachmentConfiguration> attachingProjectsMap = missingDM.getAttachingProjects();
						if (!attachingProjectsMap.isEmpty() && !attachingProjectsMap.containsKey(sourceP)) {
							ProjectAttachmentConfiguration config = attachingProjectsMap.values().iterator().next();
							missingDirectAttachments.put(missingP, config);
							MDAbstractProjectUsage e = null;
								
							if (missingP instanceof LocalAttachedProject) {
								e = createLocalMissingEdge(u, v, (LocalAttachedProject) missingP);
							} else if (missingP instanceof TeamworkAttachedProject) {
								e = createTeamworkMissingEdge(u, v, (TeamworkAttachedProject) missingP);
							} else 
								throw new IllegalArgumentException("unhandled AbstractAttachedProject: " + missingP.getClass().getName());
							createEdge(e);
						}
					}
				}
			}
		}
		
		if (performanceLoggingEnabled){ 
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check missing direct attachments: " + (currentTime-previousTime) + " (ms)");
			previousTime = System.currentTimeMillis();
		};
		
		// force loading all diagrams only if there are proxies so that the user can find the proxy usage references in the diagrams.
		final Collection<DiagramPresentationElement> projectDiagrams = project.getDiagrams();
		diagramCount = projectDiagrams.size();

		final ProxyManager proxyManager = project.getProxyManager();
		if (! proxyManager.getProxies().isEmpty()) {

			final Set<DiagramPresentationElement> unloadedDPEs = new HashSet<DiagramPresentationElement>();
			for (DiagramPresentationElement dpe : projectDiagrams) {
				if (dpe.isLoaded()) continue;
				unloadedDPEs.add(dpe);
			}

			if (! unloadedDPEs.isEmpty()) {
				final File logTraceContractsDir = plugin.getLogTraceContractsFolder();
				final File diagramProxyUsageMonitorSpec = (null == logTraceContractsDir) ? null : new File(logTraceContractsDir.getAbsolutePath() + File.separator + "diagramProxyUsageMonitor.txt");
				final String diagramProxyUsageMonitorPath = (null == diagramProxyUsageMonitorSpec || !diagramProxyUsageMonitorSpec.canRead()) 
						? null 
								: diagramProxyUsageMonitorSpec.getAbsolutePath();

				new RunnableSessionWrapper(String.format("Loading %d diagrams('%s')", unloadedDPEs.size(), project.getName())) {

					@Override
					public void run() {
						boolean wasLocked = EnvironmentLockManager.isLocked();
						try {
							EnvironmentLockManager.setLocked(true);
							for (final DiagramPresentationElement diagramPresentationElement : unloadedDPEs) {
								String sessionLabel = String.format("LoadDiagram('%s' {ID=%s})",
										diagramPresentationElement.getDiagram().getQualifiedName(),
										diagramPresentationElement.getID());
								RunnableSessionWrapper sessionWrapper = new RunnableSessionWrapper(sessionLabel) {

									@Override
									public void run() {
										diagramPresentationElement.ensureLoaded();
									}
								};

								String sessionLogFile = appender.getFileLocation(sessionWrapper.sessionID);
								if (null != diagramProxyUsageMonitorPath) {
									Monitor sessionMonitor = new Monitor(diagramProxyUsageMonitorPath, sessionLogFile);
									SessionReport report = sessionMonitor.verifyWholeSession(sessionLabel);
									if (report.numberOfViolations() > 0) {
										diagram2sessionReport.put(diagramPresentationElement, report);
									}
								}
							}
						} finally {
							EnvironmentLockManager.setLocked(wasLocked);
						}
					}
				};
			}

			for (DiagramPresentationElement dpe : projectDiagrams) {
				List<PresentationElement> manipulatedElements = new ArrayList<PresentationElement>();
				dpe.collectSubPresentationElements(manipulatedElements);
				Set<Element> dpeProxies = new HashSet<Element>();
				for (PresentationElement manipulatedElement : manipulatedElements) {
					Element e = manipulatedElement.getElement();
					if (proxyManager.isElementProxy(e)) {
						dpeProxies.add(e);
					}
				}
				if (dpeProxies.isEmpty())
					continue;
				diagram2proxyUsages.put(dpe, dpeProxies);
			}
		}
		
		if (performanceLoggingEnabled){  
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check proxies: " + (currentTime-previousTime) + " (ms)"); 
			previousTime = System.currentTimeMillis();

		};
		
		// Check profiles (This could be slow so need to be careful)
		List <Profile> projectProfiles = StereotypesHelper.getAllProfiles(project);
		
		if (!projectProfiles.isEmpty()){
			HashMap <String, Profile> profileNames = new HashMap <String,Profile>();
			HashMap <String, Profile> profileURIs = new HashMap <String, Profile>();
			
			for (Profile profile : projectProfiles){
				String profileURI = profile.getURI();
				if (profileNames.containsKey(profile.getName())){
					nonUniqueNamesProfiles.put(profile, profileNames.get(profile.getName()));
				}
				
				if (profileURIs.containsKey(profileURI)){
					nonUniqueURIProfiles.put(profile, profileURIs.get(profileURI));
				}
				
				for (String URI : profileURIs.keySet()){
					if (profileURI.length() == 0){
						break;
					}
					
					if (URI.length() == 0){
						continue;
					}
					
					if (URI.length() <= profileURI.length()){
						if (SSCAEProfileValidation.appendSeparator(profileURI).startsWith(SSCAEProfileValidation.appendSeparator(URI))){
							nonUniqueURIProfiles.put(profile, profileURIs.get(URI));
							break;
						}
					} else {
						if (SSCAEProfileValidation.appendSeparator(URI).startsWith(SSCAEProfileValidation.appendSeparator(profileURI))){
							nonUniqueURIProfiles.put(profile, profileURIs.get(URI));
							break;		
						}
					}
				}
				
				
				profileNames.put(profile.getName(), profile);
				if (!profileURI.equals("")){
					profileURIs.put(profileURI, profile);
				} else {
					// may want to check to make sure all profiles have URIs?
				}
			}
		}
		
		if (performanceLoggingEnabled){  
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check uniqueness of profiles: " + (currentTime-previousTime) + " (ms)"); 
			previousTime = System.currentTimeMillis();
		};

		// Check packages (This will be slow, need to be very careful)		
		if (!this.managedSharedPackages.isEmpty()){
			HashMap <String, Package> packageURIs = new HashMap <String, Package>();
			
			for (Package pack : this.managedSharedPackages){
				String packageURI = pack.getURI();
				
				if (packageURIs.containsKey(packageURI)){
					nonUniqueURIPackages.put(pack, packageURIs.get(packageURI));
				}
				
				for (String URI : packageURIs.keySet()){
					if (packageURI.length() == 0){
						break;
					}
					
					if (URI.length() == 0){
						continue;
					}
					
					if (URI.length() <= packageURI.length()){
						if (SSCAEProfileValidation.appendSeparator(packageURI).startsWith(SSCAEProfileValidation.appendSeparator(URI))){
							if (!MD_EXCLUDE_URI_CHECK_PROFILES_AND_PACKAGES.contains(pack.getQualifiedName())){
								nonUniqueURIPackages.put(pack, packageURIs.get(URI));
							}
							break;
						}
					} else {
						if (SSCAEProfileValidation.appendSeparator(URI).startsWith(SSCAEProfileValidation.appendSeparator(packageURI))){
							if (!MD_EXCLUDE_URI_CHECK_PROFILES_AND_PACKAGES.contains(pack.getQualifiedName())){
								nonUniqueURIPackages.put(pack, packageURIs.get(URI));
							}
							break;		
						}
					}
				}
				
				if (!packageURI.equals("")){
					packageURIs.put(packageURI, pack);
				} else {
					// may want to check to make sure all profiles have URIs?
				}
			}
		}
		
		
		if (performanceLoggingEnabled){ 
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check uniqueness of packages: " + (currentTime-previousTime) + " (ms)"); 
			previousTime = System.currentTimeMillis();
		};

		final Collection<Element> proxies = proxyManager.getProxies();
		proxyCount = proxies.size();

		for (Element proxy : proxies) {
			Assert.isTrue(proxyManager.isElementProxy(proxy));

			if (proxyManager.isGhostProxy(proxy)) {
				Element owner = proxy.getOwner();
				while (null != owner && proxyManager.isElementProxy(owner)) {
					owner = owner.getOwner();
				}
				if (null == owner) {
					proxyGhostOtherCount.add(proxy);
				} else {
					proxyGhostOkCount++;
					if (!owner2proxiesMap.containsKey(owner))
						owner2proxiesMap.put(owner, new TreeSet<Element>(ELEMENT_ID_COMPARATOR));
					SortedSet<Element> ownedProxies = owner2proxiesMap.get(owner);
					ownedProxies.add(proxy);
					owner2proxiesMap.put(owner, ownedProxies);
				}
				IAttachedProject p = proxyManager.getModuleWithMissingShare(proxy);
				MDAbstractProject ap = (null != p) ? vertexMap.get(p) : null;
				if (null != p) {
					moduleWithMissingShares.add(ap);
				}
			} else {
				IAttachedProject proxyProject = ProjectUtilities.getAttachedProject(proxy);

				// @see https://support.nomagic.com/browse/MDUMLCS-9157
				// for now, we will assume that if the proxyProject is not loaded, then MD could load it.
				// if we explicitly check if the proxyProject can be loaded --i.e. proxyProject.canLoad() --
				// then we may induce a deadlock if the graph analysis is performed during the ProjectEventListenerAdapter#projectOpened() callback.
				// This happened for OPALS!

				if (null != proxyProject && !proxyProject.isLoaded()) {
					proxyUnloadedOkCount++;
					if (!unloaded2proxiesMap.containsKey(proxyProject)) 
						unloaded2proxiesMap.put(proxyProject, new TreeSet<Element>(ELEMENT_ID_COMPARATOR));
					SortedSet<Element> proxyProjectElements = unloaded2proxiesMap.get(proxyProject);
					proxyProjectElements.add(proxy);
				} else {
					proxyUnloadedOtherCount.add(proxy);
				}
			}
		}
		
		if (performanceLoggingEnabled){
			currentTime = System.currentTimeMillis();
			pluginLog.info("PUIC -- Check for unloaded projects: " + (currentTime-previousTime) + " (ms)"); 
			previousTime = System.currentTimeMillis();
		};

		// Elaborate the graph diagnostics summary
		boolean notifySSCAE = false;

		gDiagnostic.append(String.format("'%s'\nSSCAEProjectUsageGraph(Vertices=%d, Edges=%d, Diagrams=%d)", 
				project.getName(),
				projectUsageDirectedMultigraph.vertexSet().size(),
				projectUsageDirectedMultigraph.edgeSet().size(),
				diagramCount));

		if (isProjectMissingSystemOrStandardProfileFlag()) {
			gDiagnostic.append(String.format("\nERROR: this project should have the System/Standard Profile flag set"));
		}

		if (localModulesWithTeamworkIDs.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: no local modules with teamwork project IDs"));
		} else {
			gDiagnostic.append(String.format("\nWARNING: %d local modules have a teamwork project ID",
					localModulesWithTeamworkIDs.size()));
		}
		
		if (missingDirectAttachments.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: no missing direct ProjectUsage mount attachments"));
		} else {
			gDiagnostic.append(String.format("\nERROR: this project is missing %d direct ProjectUsage mount attachments",
					missingDirectAttachments.size()));
		}

		if (unresolvedUsageEdges.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: no unresolved ProjectUsage relationships"));
		} else {
			gDiagnostic.append(String.format("\nERROR: there are %d unresolved ProjectUsage relationships -- *** please save/commit and re-open the project to force resolution ***",
					unresolvedUsageEdges.size()));
		}

		if (proxyCount == 0) {
			gDiagnostic.append(String.format("\n   OK: no proxies detected"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %s proxies detected", proxyCount)); 
			if (proxyGhostOkCount == 0)
				gDiagnostic.append(String.format("\n   OK: - no proxies for recovered elements"));
			else
				gDiagnostic.append(String.format("\nERROR: - %s proxies for recovered elements", proxyGhostOkCount));
			if (!proxyGhostOtherCount.isEmpty()) {
				gDiagnostic.append(String.format("\nERROR: - %s proxies for non-recovered elements (*** Notify SSCAE ***)", proxyGhostOtherCount.size()));
				notifySSCAE = true;
			}
			if (proxyUnloadedOkCount == 0)
				gDiagnostic.append(String.format("\n   OK: - no proxies for missing elements in loadable modules"));
			else
				gDiagnostic.append(String.format("\nERROR: - %s proxies for missing elements in loadable modules", proxyUnloadedOkCount));
			if (!proxyUnloadedOtherCount.isEmpty()) {
				gDiagnostic.append(String.format("\nERROR: - %s proxies for missing elements elsewhere (*** Notify SSCAE ***)", proxyUnloadedOtherCount.size()));
				notifySSCAE = true;
			}
			if (diagram2proxyUsages.isEmpty()) {
				gDiagnostic.append(String.format("\n   OK: none of the %d diagrams have proxy usage problems", diagramCount));
			} else {
				gDiagnostic.append(String.format("\nERROR: %d / %d diagrams have proxy usage problems", 
						diagram2proxyUsages.size(), diagramCount));
			}
		}

		if (missingProjects.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: all projects are available"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d projects are missing",
					missingProjects.size()));
		}

		if (moduleWithMissingShares.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: all projects have no missing shares"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d projects have missing shares",
					moduleWithMissingShares.size()));
		}

		if (shouldBeSystemOrStandardProfile.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: all local projects used from MD's install folder have the Standard/System Profile flag set"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d projects used from MD's install folder do not have the Standard/System Profile flag set",
					shouldBeSystemOrStandardProfile.size()));
		}
		
		if (nonUniqueNamesProfiles.isEmpty()){
			gDiagnostic.append(String.format("\n   OK: all profiles have unique names"));

		} else {
			gDiagnostic.append(String.format("\nERROR: %d profiles have non-unique names", nonUniqueNamesProfiles.size()));
		}
		
		if (nonUniqueURIProfiles.isEmpty()){
			gDiagnostic.append(String.format("\n   OK: all profiles have unique URIs"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d profiles have non-unique URIs", nonUniqueURIProfiles.size()));
		}
		
		if (nonUniqueURIPackages.isEmpty()){
			gDiagnostic.append(String.format("\n   OK: all packages have unique URIs"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d packages have non-unique URIs", nonUniqueURIPackages.size()));
		}

		if (stronglyConnectedVertices.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: project usage mount relationships are acyclic"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d projects are involved in %d project usage mount cyclic relationships", 
					stronglyConnectedVertices.size(),
					stronglyConnectedEdges.size()));
		}

		if (inconsistentUsageEdges.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: project usage mount relationships are consistent"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d project usage mount relationships are inconsistent", inconsistentUsageEdges.size()));
		}

		if (inconsistentlyUsedVertices.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: all projects are used consistently"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d projects are used inconsistently", inconsistentlyUsedVertices.size()));
		}

		if (invalidUsageEdges.isEmpty()) {
			gDiagnostic.append(String.format("\n   OK: project usage mount relationships are valid"));
		} else {
			gDiagnostic.append(String.format("\nERROR: %d project usage mount relationships are invalid",  invalidUsageEdges.size()));
		}

		notifySSCAE = createSerialization(allSortedProjects, notifySSCAE);
		
		if (notifySSCAE) {
			pluginLog.error(String.format("*** Notify SSCAE ***\n====Diagnostic:\n%s\n====\n\n====Serialization:\n%s\n====\n", 
					gDiagnostic.toString(),
					gSerialization.toString()));
		}

		gSignature.append(gSerialization.toString());

		final DOTExporterWithLegend<MDAbstractProject, MDAbstractProjectUsage> dotExporter =
				new DOTExporterWithLegend<MDAbstractProject, MDAbstractProjectUsage>(
						VERTEX_ID_PROVIDER,
						VERTEX_LABEL_PROVIDER,
						EDGE_LABEL_PROVIDER,
						VERTEX_ATTRIBUTE_PROVIDER,
						EDGE_ATTRIBUTE_PROVIDER) {
			@Override
			protected void legend(PrintWriter out) {
				writeProjectUsageGraphLegend(out);
			}
		};

		dotExporter.export(gDOT, projectUsageDirectedMultigraph);

		final DOTExporterWithLegend<MDAbstractProject, MDAbstractProjectUsage> teamworkDotExporter =
				new DOTExporterWithLegend<MDAbstractProject, MDAbstractProjectUsage>(
						VERTEX_ID_PROVIDER,
						VERTEX_LABEL_PROVIDER,
						EDGE_LABEL_PROVIDER,
						VERTEX_ATTRIBUTE_PROVIDER,
						EDGE_ATTRIBUTE_PROVIDER) {
			@Override
			protected void legend(PrintWriter out) {
				writeProjectTeamworkUsageGraphLegend(out);
			}
		};

		teamworkDotExporter.export(gDOTteamworkOnly, 
				new MaskSubgraph<MDAbstractProject, MDAbstractProjectUsage>(
						projectUsageDirectedMultigraph,
						new MaskFunctor<MDAbstractProject, MDAbstractProjectUsage>() {

							@Override
							public boolean isEdgeMasked(MDAbstractProjectUsage e) {
								return (e instanceof MDLocalProjectUsage);
							}

							@Override
							public boolean isVertexMasked(MDAbstractProject v) {
								return (v instanceof MDLocalProject);
							}

						}));
	}

	protected boolean createSerialization(final List<IProject> allSortedProjects, boolean notifySSCAE) {

		SSCAEProjectDigest digest = new SSCAEProjectDigest();

		digest.setName(project.getName());
		digest.setClassification(projectClassification);
		digest.setDiagramCount(diagramCount);
		
		for (IProject aProject : allSortedProjects) {
			MDAbstractProject p1 = lookupVertex(aProject);
			digest.getAllSortedProjects().add(p1);
		}

		for (IProject aProject : allSortedProjects) {
			MDAbstractProject p2 = lookupVertex(aProject);
			List<MDAbstractProjectUsage> p2UsedBy = vertexUsedByEdges.get(p2);
			digest.getAllUsedByRelationships().put(p2, p2UsedBy);
		}

		digest.setProjectMissingSystemOrStandardProfileFlag(isProjectMissingSystemOrStandardProfileFlag());

		for (Map.Entry<IAttachedProject, ProjectAttachmentConfiguration> missingEntry : missingDirectAttachments.entrySet()) {
			IAttachedProject missingP = missingEntry.getKey();
			MDAbstractProject missingV = lookupVertex(missingP);
			digest.getMissingDirectAttachments().add(missingV);
		}

		for (MDAbstractProjectUsage unresolvedUsage : unresolvedUsageEdges) {
			digest.getUnresolvedUsageEdges().add(unresolvedUsage);
		}

		if (proxyCount > 0) {
			for(Map.Entry<Element, SortedSet<Element>> entry : owner2proxiesMap.entrySet()) {
				SortedSet<Element> ownedProxies = entry.getValue();
				Element owner = entry.getKey();
				digest.addRecoveredElementProxy(owner, ownedProxies);
			}

			if (!proxyGhostOtherCount.isEmpty()) {
				for (Element proxy : proxyGhostOtherCount) {
					digest.addGhostElementProxy(proxy);
				}
				notifySSCAE = true;
			}

			if (proxyUnloadedOkCount > 0) {
				for (Map.Entry<IAttachedProject, SortedSet<Element>> entry : unloaded2proxiesMap.entrySet()) {
					IAttachedProject unloadedProject = entry.getKey();
					SortedSet<Element> unloadedProxies = entry.getValue();
					digest.addMissingElementsInUnloadedModule(vertexMap.get(unloadedProject), unloadedProxies);
				}
			}
			if (!proxyUnloadedOtherCount.isEmpty()) {
				for (Element proxy : proxyUnloadedOtherCount) {
					digest.getOtherMissingProxies().add(new ElementProxyInfo(proxy));
				}
				notifySSCAE = true;
			}

			for(Map.Entry<DiagramPresentationElement, Set<Element>> entry : diagram2proxyUsages.entrySet()) {
				digest.getDiagramProxyUsageProblems().add(new DiagramProxyUsageProblems(entry));
			}
		}

		for(MDAbstractProject v : missingProjects) {
			digest.getMissingProjects().add(v);
		}

		for (MDAbstractProject v : moduleWithMissingShares) {
			digest.getModulesWithMissingShares().add(v);
		}

		for (MDAbstractProject v : shouldBeSystemOrStandardProfile) {
			digest.getShouldBeSystemOrStandardProfile().add(v);
		}

		if (nonUniqueNamesProfiles.isEmpty()){

		} else {
			gMessages.append(String.format("\nERROR: %d profiles have non-unique names", nonUniqueNamesProfiles.size()));
			for (Profile p : nonUniqueNamesProfiles.keySet()){
				gMessages.append("\nProfiles with the same name:");
				gMessages.append("\n -" + p.getQualifiedName());
				gMessages.append("\n -" + nonUniqueNamesProfiles.get(p).getQualifiedName());
			}
		}

		if (nonUniqueURIProfiles.isEmpty()){
		} else {
			gMessages.append(String.format("\nERROR: %d profiles have non-unique URIs", nonUniqueURIProfiles.size()));
			for (Profile p : nonUniqueURIProfiles.keySet()){
				gMessages.append("\nProfiles with the same URI:");
				gMessages.append("\n -" + p.getQualifiedName());
				gMessages.append("\n -" + nonUniqueURIProfiles.get(p).getQualifiedName());
			}
		}
		
		if (nonUniqueURIPackages.isEmpty()){
		} else {
			gSerialization.append(String.format("\nERROR: %d packages have non-unique URIs", nonUniqueURIPackages.size()));
			for (Package p : nonUniqueURIPackages.keySet()){
				gSerialization.append("\nPackages with the same URI:");
				gSerialization.append("\n -" + p.getQualifiedName());
				gSerialization.append("\n -" + nonUniqueURIPackages.get(p).getQualifiedName());
			}
		}

		if (nonUniqueURIPackages.isEmpty()){
		} else {
			gMessages.append(String.format("\nERROR: %d packages have non-unique URIs", nonUniqueURIPackages.size()));
			for (Package p : nonUniqueURIPackages.keySet()){
				gMessages.append("\nPackages with the same URI:");
				gMessages.append("\n -" + p.getQualifiedName());
				gMessages.append("\n -" + nonUniqueURIPackages.get(p).getQualifiedName());
			}
		}

		for (MDAbstractProject v : stronglyConnectedVertices) {
			digest.getStronglyConnectedVertices().add(v);
		}
		for (MDAbstractProjectUsage e : stronglyConnectedEdges) {
			digest.getStronglyConnectedEdges().add(e);
		}

		for (MDAbstractProjectUsage e : inconsistentUsageEdges) {
			digest.getInconsistentUsageEdges().add(e);
		}

		for (MDAbstractProject v : inconsistentlyUsedVertices) {
			digest.getInconsistentlyUsedVertices().add(v);
		}

		for (MDAbstractProjectUsage e : invalidUsageEdges) {
			digest.getInvalidUsageEdges().add(e);
		}
		
		gSerialization.append(YamlDigestHelper.serialize(digest));
		
		return notifySSCAE;
	}
	
	public static enum DOTImageFormat { png, jpg, gif, svg }

	public static Map<DOTImageFormat, String> DOTImageFormatName = new HashMap<DOTImageFormat, String>();

	static {
		DOTImageFormatName.put(DOTImageFormat.gif, "gif");
		DOTImageFormatName.put(DOTImageFormat.jpg, "jpg");
		DOTImageFormatName.put(DOTImageFormat.png, "png");
		DOTImageFormatName.put(DOTImageFormat.svg, "svg");
	}

	public File convertAllUsageDOTgraph() throws IOException {
		return convertDOTgraphInternal(getProjectUsageGraphDOTExport().getBytes());
	}

	public File convertTeamworkUsageDOTgraph() throws IOException {
		return convertDOTgraphInternal(getProjectTeamworkUsageGraphDOTExport().getBytes());
	}

	public File convertDOTgraphInternal(@Nonnull byte[] bytes) throws IOException {

		File pugTemp = ApplicationEnvironment.getTempDir("ProjectUsageGraphs");
		if (null != pugTemp) {
			pluginLog.info(String.format("%s - convertDOTgraph - project '%s' temporary directory:\n'%s'", pluginName, project.getName(), pugTemp));
			File pugDOT = new File(pugTemp.getAbsoluteFile() + File.separator + project.getID() + ".gv");
			pluginLog.info(String.format("%s - convertDOTgraph - project '%s' gv file (in temp. dir): '%s'", pluginName, project.getName(), pugDOT.getName()));
			FileOutputStream out = new FileOutputStream(pugDOT);
			out.write(bytes);
			out.close();

			return pugDOT;
		}

		return null;
	}

	public BufferedImageFile convertDOTFile(@Nonnull File pugDOT, @Nonnull DOTImageFormat dotImageFormat) throws IIOException, IOException, InterruptedException {
		String dotCommand = ProjectUsageIntegrityPlugin.getInstance().getDOTexecutablePath();
		if (null == dotCommand) 
			return null;

		File pugTemp = pugDOT.getParentFile();
		File pugImage = new File(pugTemp.getAbsoluteFile() + File.separator + project.getID() + "." + DOTImageFormatName.get(dotImageFormat));
		if (pugImage.exists()) {
			pluginLog.info(String.format("%s - convertDOTFile - deleting previous image for '%s' : '%s'", pluginName, project.getName(), pugImage.getName()));
			pugImage.delete();
		}
			
		CommandLine cmdLine = new CommandLine(dotCommand);
		cmdLine.addArgument("-Tpng");
		cmdLine.addArgument("-o");
		cmdLine.addArgument(pugImage.getName());
		cmdLine.addArgument(pugDOT.getName());

		pluginLog.info(String.format("%s - convertDOTgraph - converting gv to image for '%s'", pluginName, project.getName()));

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);

		// consider '0' exit value as success.
		Executor executor = new DefaultExecutor();
		executor.setExitValue(0);
		executor.setWatchdog(watchdog);
		executor.setWorkingDirectory(pugTemp);
		executor.execute(cmdLine, resultHandler);

		resultHandler.waitFor();

		if (!executor.isFailure(resultHandler.getExitValue())) {
			pluginLog.info(String.format("%s - convertDOTgraph - reading image for '%s' from: '%s'", pluginName, project.getName(), pugImage.getName()));
			BufferedImageFile imageFile = new BufferedImageFile(pugImage);

			pluginLog.info(String.format("%s - convertDOTgraph - got image for '%s'", pluginName, project.getName()));
			return imageFile;
		}

		return null;
	}
	
	/**
	 * @param pugDOT gv file
	 * @return true if the graphviz application was opened successfully for the gv file.
	 * @throws IIOException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean openDOTFileWithGraphViz(@Nonnull File pugDOT) throws IIOException, IOException, InterruptedException {
		String graphvizApp = ProjectUsageIntegrityPlugin.getInstance().getGraphvizApplicationPath();
		if (null == graphvizApp) 
			return false;

		File pugTemp = pugDOT.getParentFile();
			
		CommandLine cmdLine;
		
		switch (SSCAEProjectUsageIntegrityOptions.getCurrentPlatform()) {
		case LINUX:
			cmdLine = new CommandLine(graphvizApp);
			break;
		case MACOSX:
			cmdLine = new CommandLine("/usr/bin/open");
			cmdLine.addArgument("-a");
			cmdLine.addArgument(graphvizApp);
			break;
		case WINDOWS:
			cmdLine = new CommandLine("cmd");
			cmdLine.addArgument("/c");
			cmdLine.addArgument("start");
			cmdLine.addArgument(graphvizApp);
			break;
		default:
			return false;
		}
		cmdLine.addArgument(pugDOT.getName());

		pluginLog.info(String.format("%s - openDOTFileWithGraphViz - opening DOT file for project: '%s'", pluginName, project.getName()));

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);

		// consider '0' exit value as success.
		Executor executor = new DefaultExecutor();
		executor.setExitValue(0);
		executor.setWatchdog(watchdog);
		executor.setWorkingDirectory(pugTemp);
		executor.execute(cmdLine, resultHandler);

		resultHandler.waitFor();

		if (executor.isFailure(resultHandler.getExitValue())) {
			pluginLog.error(String.format("%s - openDOTFileWithGraphViz - error while opening DOT file for project '%s' from: '%s'", pluginName, project.getName(), pugDOT.getAbsolutePath()),
							resultHandler.getException());
			return false;
		}
		
		pluginLog.info(String.format("%s - openDOTFileWithGraphViz - opened DOT file for project '%s' from: '%s'", pluginName, project.getName(), pugDOT.getAbsolutePath()));
		return true;
	}

	public void createEdge(MDAbstractProjectUsage usage) {
		if (includeLocalEdges || usage instanceof MDTeamworkProjectUsage) {
			MDAbstractProject v1 = usage.getSource();
			projectUsageDirectedMultigraph.addVertex(v1);

			MDAbstractProject v2 = usage.getTarget();
			projectUsageDirectedMultigraph.addVertex(v2);

			projectUsageDirectedMultigraph.addEdge(v1, v2, usage);
		}
	}

	protected final VertexNameProvider<MDAbstractProject> VERTEX_ID_PROVIDER = new VertexNameProvider<MDAbstractProject>() {
		public String getVertexName(MDAbstractProject v) { return v.getIndex(); }
	};

	protected final VertexNameProvider<MDAbstractProject> VERTEX_LABEL_PROVIDER = new VertexNameProvider<MDAbstractProject>() {
		public String getVertexName(MDAbstractProject v) {
			boolean isInconsistentlyUsed = SSCAEProjectUsageGraph.this.inconsistentlyUsedVertices.contains(v);
			String usageLabel = SSCAEProjectUsageGraph.this.vertexUsageConsistencyLabel.get(v);
			
			String vNameLabel = (SSCAEProjectUsageGraph.this.useAnonymousVertexLabels) ? "" : v.getName();
			
			MDTeamworkProject tv = (v instanceof MDTeamworkProject) ? (MDTeamworkProject) v : null;
			String tvAnonymizedVersion = (tv == null) ? "" : tv.getAnonymizedVersion();
			String tvFullVersion = (tv == null) ? "" : tv.getFullVersion();
			
			if (v instanceof MDTeamworkPrimaryProject && primaryProject instanceof TeamworkPrimaryProject) {
				TeamworkPrimaryProject primaryTProject = (TeamworkPrimaryProject) primaryProject;
				
				final IVersionDescriptor version = ProjectUtilities.getVersion(primaryTProject);
				String versionFragment = "#" + version.getName();
				tvAnonymizedVersion = v.getIndex() + versionFragment;
				
				final String teamworkRemoteId = ProjectUtilities.getTeamworkRemoteId(primaryTProject);
				com.nomagic.magicdraw.core.project.ProjectDescriptor descriptor;
				try {
					descriptor = ((teamworkRemoteId == null) ? null : TeamworkUtils.getRemoteProjectDescriptor(teamworkRemoteId));
					if (null == descriptor) {
						tvFullVersion = teamworkRemoteId + versionFragment;
					} else {
						tvFullVersion = ProjectDescriptorsFactory.getProjectFullPath(descriptor.getURI()) + versionFragment;
					}
				} catch (RemoteException e) {
					descriptor = null;
					tvFullVersion = "[server not available]" + versionFragment;
				}
			}
			String tvNameLabel = (tv == null) ? "" : ((SSCAEProjectUsageGraph.this.useAnonymousVertexLabels) ? tvAnonymizedVersion : tvFullVersion);
			String vName = (null == tv) ? vNameLabel : tvNameLabel;
			if (v.getClassification() != null) {
				vName = vName + String.format("\\n{classification=%s}", ProjectClassificationShortLabel.get(v.getClassification()));
			}
			
			StringBuffer vertexLabel = new StringBuffer();
			if (isInconsistentlyUsed || null == usageLabel || usageLabel.isEmpty())	
				vertexLabel.append(String.format("[%s] %s", v.getIndex(), vName));
			else 
				vertexLabel.append(String.format("[%s] %s\\n%s", v.getIndex(), vName, SSCAEProjectUsageGraph.this.vertexUsageConsistencyLabel.get(v)));
			if (v instanceof MDTeamworkProject) {
				StringBuffer tagBuffer = new StringBuffer();
				boolean first = true;
				for (String tag : tv.getTags()) {
					if (first) { first = false; tagBuffer.append("\\n"); } else { tagBuffer.append(", "); }
					Matcher match = ProjectUsageIntegrityPlugin.SSCAE_CLEAN_MD5_TAG_REPLACE.matcher(tag);
					tag = match.matches() ? "<SSCAE Clean>" : tag;
					tagBuffer.append(String.format("[%s]", tag));
				}
				vertexLabel.append(tagBuffer.toString());
			}
			return vertexLabel.toString();
		}
	};

	protected final EdgeNameProvider<MDAbstractProjectUsage> EDGE_LABEL_PROVIDER = new EdgeNameProvider<MDAbstractProjectUsage>() {
		public String getEdgeName(MDAbstractProjectUsage e) { 
			if (SSCAEProjectUsageGraph.this.inconsistentUsageEdges.contains(e))
				return e.getSignature(); 
			else
				return "";
		}
	};

	protected final ComponentAttributeProvider<MDAbstractProject> VERTEX_ATTRIBUTE_PROVIDER = new ComponentAttributeProvider<MDAbstractProject>() {
		public Map<String, String> getComponentAttributes(MDAbstractProject v) {
			Map<String, String> attribs = new LinkedHashMap<String, String>();
			if (stronglyConnectedVertices.contains(v) || missingProjects.contains(v) || moduleWithMissingShares.contains(v)) {
				if (!missingProjects.contains(v) && !moduleWithMissingShares.contains(v)) {
					attribs.put("shape", ((v instanceof MDTeamworkProject) ? "doubleoctagon" : "rectangle"));
				} else {
					attribs.put("shape", ((v instanceof MDTeamworkProject) ? "tripleoctagon" : "note"));
				}
				attribs.put("color", "red");
			} else {
				String color = "black";
				if (v instanceof MDAttachedProject && v.isHybrid()) {
					color = "brown";
				}
				attribs.put("shape", ((v instanceof MDTeamworkProject) ? "octagon" : "tab"));
				attribs.put("color", color);
			}
			MDAbstractProject pv = helper.getPreviousProjectVertexMatching(v);
			if (v instanceof MDLocalProject) {
				MDLocalProject lv = (MDLocalProject) v;
				if (lv.hasTeamworkProjectID()) {
					attribs.put("style", "filled");
					attribs.put("fillcolor", "pink");
				} else if (pv != null) {
					if (!(pv instanceof MDLocalProject) ||
						!((MDLocalProject) pv).getMD5checksum().equals(lv.getMD5checksum())) {
						attribs.put("style", "filled");
						attribs.put("fillcolor", "yellow");
					}
				}
			} else if (v instanceof MDTeamworkProject) {
				MDTeamworkProject tv = (MDTeamworkProject) v;
				if (!(pv instanceof MDTeamworkProject) ||
					!((MDTeamworkProject) pv).getFullVersion().equals(tv.getFullVersion())) {
						attribs.put("style", "filled");
						attribs.put("fillcolor", "yellow");
				}
				if (tv instanceof MDTeamworkPrimaryProject && primaryProject instanceof TeamworkPrimaryProject) {
					TeamworkPrimaryProject primaryTProject = (TeamworkPrimaryProject) primaryProject;
					if (!primaryTProject.isHistorical() && !primaryTProject.isUpToDate()) {
						attribs.put("style", "filled");
						attribs.put("fillcolor", "yellow");
					}
				}
			}
			
			return attribs;
		}
	};

	protected final ComponentAttributeProvider<MDAbstractProjectUsage> EDGE_ATTRIBUTE_PROVIDER = new ComponentAttributeProvider<MDAbstractProjectUsage>() {
		public Map<String, String> getComponentAttributes(MDAbstractProjectUsage e) {
			Map<String, String> attribs = new LinkedHashMap<String, String>();
			if (e instanceof MDTeamworkProjectUsage) {
				MDTeamworkProjectUsage te = (MDTeamworkProjectUsage) e;
				attribs.put("style", ((te.isSticky()) ? "solid" : "dashed"));
			} else if (e instanceof MDTeamworkProjectMissingUsage) {
				attribs.put("style", "dashed");
			} else {
				attribs.put("style", "dotted");
			}
			if (e.isReshared())
				attribs.put("arrowhead", ((e.isReadOnly()) ? "normal" : "empty"));
			else
				attribs.put("arrowhead", ((e.isReadOnly()) ? "dot" : "odot"));

			if (stronglyConnectedEdges.contains(e) || inconsistentUsageEdges.contains(e) || invalidUsageEdges.contains(e) 
					|| (e instanceof MDLocalProjectMissingUsage) 
					|| (e instanceof MDTeamworkProjectMissingUsage)) {
				attribs.put("color", "red:red");
			} else {
				attribs.put("color", "black");
			}
			return attribs;
		}
	};

	public MDAbstractProject lookupVertex(IProject p) {
		if (null == p)							throw new IllegalArgumentException("non-null project");
		if (!vertexMap.containsKey(p))			throw new IllegalArgumentException("project should be already in the vertex map");
		return vertexMap.get(p);
	}

	public MDAbstractProjectUsage createLocalMissingEdge(MDAbstractProject v1, MDAbstractProject v2, LocalAttachedProject lap) {
		MDLocalProjectMissingUsage e = new MDLocalProjectMissingUsage();
		MDLocalProjectMissingUsage.configure(e, v1, v2);
		return e;
	}

	public MDAbstractProjectUsage createLocalEdge(MDAbstractProject v1, MDAbstractProject v2, ProjectUsage pu, LocalAttachedProject lap) {
		if (edgeMap.containsKey(pu))					throw new IllegalArgumentException("No duplicate ProjectUsage edges!");
		MDLocalProjectUsage e = new MDLocalProjectUsage();
		MDLocalProjectUsage.configure(e, v1, v2, pu);
		edgeMap.put(pu, e);
		return e;
	}

	public MDAbstractProjectUsage createTeamworkMissingEdge(MDAbstractProject v1, MDAbstractProject v2, TeamworkAttachedProject tap) throws RemoteException  {
		MDTeamworkProjectMissingUsage e = new MDTeamworkProjectMissingUsage();
		MDTeamworkProjectMissingUsage.configure(e, v1, v2);
		return e;
	}
	
	public MDAbstractProjectUsage createTeamworkEdge(MDAbstractProject v1, MDAbstractProject v2, ProjectUsage pu, TeamworkAttachedProject tap) throws RemoteException  {
		if (edgeMap.containsKey(pu))					throw new IllegalArgumentException("No duplicate ProjectUsage edges<!");
		MDTeamworkProjectUsage e = new MDTeamworkProjectUsage();
		MDTeamworkProjectUsage.configure(e, v1, v2, pu);
		edgeMap.put(pu, e);
		return e;
	}

	public MDAbstractProject createVertex(IProject p, int width) throws RemoteException {
		if (null == p)								throw new IllegalArgumentException("non-null project");

		if (vertexMap.containsKey(p))
			return vertexMap.get(p);

		int index = vertexMap.size();
		MDAbstractProject v = helper.createVertexInternal(p, this, index, width);

		vertexMap.put(p, v);
		vertexUsedByEdges.put(v, new ArrayList<MDAbstractProjectUsage>());

		if (v instanceof MDLocalProject && ((MDLocalProject) v).hasTeamworkProjectID())
			localModulesWithTeamworkIDs.add(v);
		
		return v;
	}
	
	public static final URI INSTALL_ROOT_URI1 = URI.createFileURI(ApplicationEnvironment.getInstallRoot());
	public static final URI INSTALL_ROOT_URI2 = URI.createURI("file:/%3Cinstall.root%3E/");
	public static final URI MD_ROOT_URI = URI.createURI("md://install_root/");

	public static URI getMDRelativeProjectURI(IProject p) {
		if (null == p) throw new IllegalArgumentException("non-null project");
		return getMDRelativeURI(p.getLocationURI());
	}

	public static boolean isLocalAttachedProjectAvailable(@Nonnull LocalAttachedProject p) {
		if (p instanceof TeamworkAttachedProject)
			return true;

		URI uri = p.getLocationURI();
		if (null == uri)
			return false;

		File f = new File(uri.toFileString());
		return (f.exists() && f.canRead());
	}

	public static String getProjectComparisonKey(@Nonnull IProject p) {
		String ID = p.getProjectID();
		if (p instanceof LocalAttachedProject) {
			if (!isLocalAttachedProjectAvailable((LocalAttachedProject) p))
				ID = "Z" + p.getName();
		}
		return String.format("%b-%s", (p instanceof ITeamworkProject), ID);
	}

	public static Comparator<IProject> IPROJECT_COMPARATOR = new Comparator<IProject>() {
		public int compare(IProject p1, IProject p2) {
			if (p1 instanceof IPrimaryProject) return -1;
			if (p2 instanceof IPrimaryProject) return 1;

			return getProjectComparisonKey(p1).compareTo(getProjectComparisonKey(p2));
		}
	};

	public static URI getMDRelativeURI(URI loc) {
		if (null == loc) 						throw new IllegalArgumentException("non-null URI");
		if ("teamwork".equals(loc.scheme()))		return loc;

		URI fileURI = loc;
		if ("file".equals(loc.scheme())) {
			File f = new File(loc.toFileString());
			try {
				fileURI = URI.createFileURI(f.getCanonicalPath());
			} catch (IOException e) {
				fileURI = loc;
			}
		}
		
		URI mdRelativeLoc = fileURI.replacePrefix(INSTALL_ROOT_URI1, MD_ROOT_URI);
		if (null == mdRelativeLoc)
			mdRelativeLoc = fileURI.replacePrefix(INSTALL_ROOT_URI2, MD_ROOT_URI);

		if (null != mdRelativeLoc) {
			String[] segments = mdRelativeLoc.segments();
			if (segments.length > 1) {
				String segment1 = segments[0];
				if (		"templates".equals(segment1) || 
						"profiles".equals(segment1) || 
						"modelLibraries".equals(segment1) || 
						"samples".equals(segment1)) {
					return mdRelativeLoc;
				}
			}
		}

		if ("file".equals(loc.scheme()))
			return URI.createFileURI(loc.lastSegment());

		return loc;
	}

	public static boolean isMDRelativeURI(URI uri) { return "md".equals(uri.scheme()) && "install_root".equals(uri.host()); }

	public static Comparator<MDAbstractProject> PROJECT_VERTEX_COMPARATOR = new Comparator<MDAbstractProject>() {
		public int compare(MDAbstractProject p1, MDAbstractProject p2) { 
			if (p1.equals(p2)) return 0;
			if (p1.isRootProject()) return -1;
			if (p2.isRootProject()) return 1;
			return p1.getIndex().compareTo(p2.getIndex()); 
		}
	};

	public static Comparator<Element> ELEMENT_ID_COMPARATOR = new Comparator<Element>() {
		public int compare(Element e1, Element e2) { 
			if (e1.equals(e2)) return 0;
			return e1.getID().compareTo(e2.getID()); 
		}
	};

	protected static String getMD5Result() {
		if (null == md5) {
			return "<no MD5 algorithm>";
		}

		synchronized(md5) {
			byte[] mdbytes = md5.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i=0;i<mdbytes.length;i++) {
				String hex=Integer.toHexString(0xff & mdbytes[i]);
				if(hex.length()==1) hexString.append('0');
				hexString.append(hex);
			}
			return  hexString.toString();
		}
	}
	public static String getMD5FromString(String s) {

		if (null == md5) {
			return "<no MD5 algorithm>";
		}

		synchronized(md5) {
			md5.reset();
			md5.update(s.getBytes());
			return getMD5Result();
		}
	}

	public static String getMD5FromFile(String filepath) throws FileNotFoundException, IOException {

		if (null == md5) {
			return "<no MD5 algorithm>";
		}

		synchronized(md5) {
			FileInputStream fis = new FileInputStream(filepath);
			byte[] dataBytes = new byte[1024];
			int nread = 0; 

			md5.reset();
			while ((nread = fis.read(dataBytes)) != -1) {
				md5.update(dataBytes, 0, nread);
			};
			return getMD5Result();
		}
	}

	public static Comparator<MDAbstractProjectUsage> PROJECT_USAGE_EDGE_COMPARATOR = new Comparator<MDAbstractProjectUsage>() {
		public int compare(MDAbstractProjectUsage pu1, MDAbstractProjectUsage pu2) { return pu1.getIndex().compareTo(pu2.getIndex()); }
	};

	public static String PROJECT_USAGE_TARGET_MISMATCH = "MDProjectUsage target mismatch:\ntarget project=%s\nusage target=%s";

	/**
	 * The meaning of the resolved flag in the MD Open API is unclear.
	 * In practice, this flag is unset when someone replaces a teamwork module with another version of that module.
	 * Without overriding this flag to true, what happens is that the user has to turn off the SSCAE ProjectUsage Integrity checker to save the project.
	 * Unless NoMagic clarifies whether we need to worry about this flag, we will be overriding to true so as to effectively ignore it.
	 * However, should we eventually care about it, then set the override flag to false.
	 */
	public static final boolean RESOLVED_FLAG_OVERRIDE = true;

	protected void writeProjectUsageGraphLegend(PrintWriter out) {
		out.println("  subgraph legend {");
		out.println("   subgraph cluster_level1 {");
		out.println("    label=\"ProjectUsage relationship (sharing & access)\\n(edge is red when cyclic)\"");
		out.println("    edge [weight=1000]");
		out.println("    subgraph cluster_shared {");
		out.println("     label=\"shared\"");
		out.println("     subgraph shared_ro {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("	     f1 [ label=\"R/O\" ]");
		out.println("        f1 -> f2 [arrowhead=normal, dir=forward]");
		out.println("     }");
		out.println("     subgraph shared_rw {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        f3 [ label=\"R/W\"]");
		out.println("        f3 -> f4 [arrowhead=empty, dir=forward]");
		out.println("     }");
		out.println("    }");
		out.println("    subgraph cluster_unshared {");
		out.println("     label=\"! shared\"");
		out.println("     subgraph unshared_ro {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        g1 [ label=\"R/O\" ]");
		out.println("        g1 -> g2 [arrowhead=dot, dir=forward]");
		out.println("     }");
		out.println("     subgraph unshared_rw {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        g3 [ label=\"R/W\"]");
		out.println("        g3 -> g4 [arrowhead=odot, dir=forward]");
		out.println("     }");
		out.println("    }");
		out.println("   }");
		out.println("   subgraph cluster_level2 {");
		out.println("    label=\"ProjectUsage relationship kind\\n(edge is doubled and red when inconsistent)\"");
		out.println("    edge [weight=1000, arrowhead=none]");
		out.println("    subgraph cluster_teamwork1 {");
		out.println("     label=\"Teamwork\"");
		out.println("     subgraph stickya {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        u1 [ label=\"sticky\\nversion\" ]");
		out.println("        u1 -> u2 [ style=solid ]");
		out.println("     }");
		out.println("     subgraph latesta {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("	v1 [ label=\"latest\\nversion\"]");
		out.println("        v1 -> v2 [ style=dashed ]");
		out.println("     }");
		out.println("    }");
		out.println("    subgraph cluster_locala {");
		out.println("     label=\"Local\"");
		out.println("     subgraph locala {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("	w1 [ label=\" \\n \" ]");
		out.println("        w1 -> w2 [ style=dotted ]");
		out.println("     }");
		out.println("    }");
		out.println("   }");
		out.println("   subgraph cluster_level3 {");
		out.println("    label=\"Model Location\"");
		out.println("    edge [weight=1000]");
		out.println("    subgraph a_room2_1 {");
		out.println("      label=\"\"");
		out.println("      pencolor=none");
		out.println("      l0 [label=\"Local\",shape=none]");
		out.println("      l1 [label=\"OK\", shape=tab, height=1]");
		out.println("      l2 [label=\"Modified\", shape=tab, height=1, style=filled, fillcolor=yellow]");
		out.println("      l3 [label=\"Hybrid\", shape=note, height=1, style=dashed]");
		out.println("      l4 [label=\"Cyclic\", shape=note, height=1, color=red]");
		out.println("      l5 [label=\"Missing\", shape=note, height=1, color=red]");
		out.println("      l6 [label=\"TeamworkID\", shape=note, height=1, style=filled, fillcolor=pink]");
		out.println("      l0 -> l1 -> l2 -> l3 -> l4 -> l5 -> l6 [style=invis]");
		out.println("    }");
		out.println("    subgraph a_room2_2 {");
		out.println("      label=\"\"");
		out.println("      pencolor=none");
		out.println("      t0 [label=\"Teamwork\",shape=none]");
		out.println("      t1 [label=\"OK\", shape=octagon, width=1]");
		out.println("      t2 [label=\"Modified\", shape=octagon, width=1, style=filled, fillcolor=yellow]");
		out.println("      t3 [label=\"Hybrid\", shape=doubleoctagon, width=1, style=dashed]");
		out.println("      t4 [label=\"Cyclic\", shape=doubleoctagon, width=1, color=red]");
		out.println("      t5 [label=\"Missing\", shape=tripleoctagon, width=1, color=red]");
		out.println("	   t0 -> t1 -> t2 -> t3 -> t4 -> t5 [style=invis]");        
		out.println("    }");
		out.println("    { rank=same; l0 -> t0 [style=invis]}");
		out.println("   }");
		out.println("   {edge[style=invis]");
		out.println("    {f2 f4 g2 g4} -> {u1 v1 w1}");
		out.println("    {u2 v2 w2} -> {l0 t0}");
		out.println("   }");
		out.println("  }");
	}

	protected void writeProjectTeamworkUsageGraphLegend(PrintWriter out) {
		out.println("  subgraph legend {");
		out.println("   subgraph cluster_level1 {");
		out.println("    label=\"ProjectUsage relationship (sharing & access)\\n(edge is red when cyclic)\"");
		out.println("    edge [weight=1000]");
		out.println("    subgraph cluster_shared {");
		out.println("     label=\"shared\"");
		out.println("     subgraph shared_ro {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("	     f1 [ label=\"R/O\" ]");
		out.println("        f1 -> f2 [arrowhead=normal, dir=forward]");
		out.println("     }");
		out.println("     subgraph shared_rw {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        f3 [ label=\"R/W\"]");
		out.println("        f3 -> f4 [arrowhead=empty, dir=forward]");
		out.println("     }");
		out.println("    }");
		out.println("    subgraph cluster_unshared {");
		out.println("     label=\"! shared\"");
		out.println("     subgraph unshared_ro {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        g1 [ label=\"R/O\" ]");
		out.println("        g1 -> g2 [arrowhead=dot, dir=forward]");
		out.println("     }");
		out.println("     subgraph unshared_rw {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        g3 [ label=\"R/W\"]");
		out.println("        g3 -> g4 [arrowhead=odot, dir=forward]");
		out.println("     }");
		out.println("    }");
		out.println("   }");
		out.println("   subgraph cluster_level2 {");
		out.println("    label=\"ProjectUsage relationship kind\\n(edge is doubled and red when inconsistent)\"");
		out.println("    edge [weight=1000, arrowhead=none]");
		out.println("    subgraph cluster_teamwork1 {");
		out.println("     label=\"Teamwork\"");
		out.println("     subgraph stickya {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("        u1 [ label=\"sticky\\nversion\" ]");
		out.println("        u1 -> u2 [ style=solid ]");
		out.println("     }");
		out.println("     subgraph latesta {");
		out.println("        node [shape=none,label=\"\",height=0]");
		out.println("	     v1 [ label=\"latest\\nversion\"]");
		out.println("        v1 -> v2 [ style=dashed ]");
		out.println("     }");
		out.println("    }");
		out.println("   }");
		out.println("   subgraph cluster_level3 {");
		out.println("    label=\"Model Location\"");
		out.println("    edge [weight=1000]");
		out.println("    subgraph a_room2_2 {");
		out.println("      label=\"\"");
		out.println("      pencolor=none");
		out.println("      t0 [label=\"Teamwork\",shape=none]");
		out.println("      t1 [label=\"OK\", shape=octagon, width=1]");
		out.println("      t2 [label=\"Modified\", shape=octagon, width=1, style=filled, fillcolor=yellow]");
		out.println("      t3 [label=\"Hybrid\", shape=doubleoctagon, width=1, style=dashed]");
		out.println("      t4 [label=\"Cyclic\", shape=doubleoctagon, width=1, color=red]");
		out.println("      t5 [label=\"Missing\", shape=tripleoctagon, width=1, color=red]");
		out.println("	   t0 -> t1 -> t2 -> t3 -> t4 -> t5 [style=invis]");        
		out.println("    }");
		out.println("   }");
		out.println("   {edge[style=invis]");
		out.println("    {f2 f4 g2 g4} -> {u1 v1}");
		out.println("    {u2 v2} -> {t0}");
		out.println("   }");
		out.println("  }");
	}

	public boolean isIsomorphicWith(SSCAEProjectUsageGraph otherGraph){
		if (otherGraph == null){
			return false;
		}

		return this.getProjectUsageGraphSignature().equals(otherGraph.getProjectUsageGraphSignature());
	}
	
	public Collection <Package> getPackages(Package root){
		
		Collection<Package> packages = new LinkedList<Package>(root.getNestedPackage());
		Collection <Package> addPackages = new LinkedList<Package>();
		for (Package pack : packages){
			addPackages.addAll(getPackages(pack));
		}
		packages.addAll(addPackages);
		return packages;
	}
}
