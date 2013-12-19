package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

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

public class SSCAEProjectDigest {

	public final static String DIGEST_FORMAT_VERION = "yaml.v1";
	
	private String digestFormatVersion;
	private String name;
	private ProjectClassification classification;
	private List<MDAbstractProject> allSortedProjects;
	private List<MDAbstractProjectUsage> allSortedUsages;
	private List<MDAbstractProject> stronglyConnectedVertices;
	private List<MDAbstractProjectUsage> stronglyConnectedEdges;
	private List<MDAbstractProject> inconsistentlyUsedVertices;
	private List<MDAbstractProjectUsage> inconsistentUsageEdges;
	private List<MDAbstractProjectUsage> invalidUsageEdges;
	private SortedMap<MDAbstractProject, List<MDAbstractProjectUsage>> allUsedByRelationships;
	private int proxyCount;
	private int diagramCount;
	
	/**
	 * should be false
	 */
	private boolean isProjectMissingSystemOrStandardProfileFlag;
	
	/**
	 * should be empty
	 */
	private List<MDAbstractProject> missingProjects;
	
	/**
	 * should be empty
	 */
	private List<MDAbstractProject> missingDirectAttachments;
	
	/**
	 * should be empty
	 */
	private List<MDAbstractProject> modulesWithMissingShares;
	
	/**
	 * should be empty
	 */
	private List<MDAbstractProjectUsage> unresolvedUsageEdges;
	
	/**
	 * should be empty
	 */
	private List<RecoveredElementProxy> recoveredElementProxies;
	
	/**
	 * should be empty
	 */
	private List<ElementProxyInfo> ghostElementProxies;
	
	/**
	 * should be empty
	 */
	private SortedMap<MDAbstractProject, List<ElementProxyInfo>> missingElementsInUnloadedModules;
	
	/**
	 * should be empty
	 */
	private List<ElementProxyInfo> otherMissingProxies;
	
	/**
	 * should be empty
	 */
	private List<DiagramProxyUsageProblems> diagramProxyUsageProblems;
	
	/**
	 * should be empty
	 */
	private List<MDAbstractProject> shouldBeSystemOrStandardProfile;
	
	public SSCAEProjectDigest() {
		
		this.setDigestFormatVersion(DIGEST_FORMAT_VERION);
		this.setAllSortedProjects(new ArrayList<MDAbstractProject>());
		this.setAllSortedUsages(new ArrayList<MDAbstractProjectUsage>());
		this.setStronglyConnectedEdges(new ArrayList<MDAbstractProjectUsage>());
		this.setStronglyConnectedVertices(new ArrayList<MDAbstractProject>());
		this.setInconsistentlyUsedVertices(new ArrayList<MDAbstractProject>());
		this.setInconsistentUsageEdges(new ArrayList<MDAbstractProjectUsage>());
		this.setInvalidUsageEdges(new ArrayList<MDAbstractProjectUsage>());
		this.setAllUsedByRelationships(new TreeMap<MDAbstractProject, List<MDAbstractProjectUsage>>(SSCAEProjectUsageGraph.PROJECT_VERTEX_COMPARATOR));
		this.setDiagramProxyUsageProblems(new ArrayList<DiagramProxyUsageProblems>());
		this.setGhostElementProxies(new ArrayList<ElementProxyInfo>());
		this.setMissingDirectAttachments(new ArrayList<MDAbstractProject>());
		this.setMissingElementsInUnloadedModules(new TreeMap<MDAbstractProject, List<ElementProxyInfo>>(SSCAEProjectUsageGraph.PROJECT_VERTEX_COMPARATOR));
		this.setMissingProjects(new ArrayList<MDAbstractProject>());
		this.setModulesWithMissingShares(new ArrayList<MDAbstractProject>());
		this.setOtherMissingProxies(new ArrayList<ElementProxyInfo>());
		this.setRecoveredElementProxies(new ArrayList<RecoveredElementProxy>());
		this.setShouldBeSystemOrStandardProfile(new ArrayList<MDAbstractProject>());
		this.setUnresolvedUsageEdges(new ArrayList<MDAbstractProjectUsage>());
	}
	
	
	public String getDigestFormatVersion() {
		return digestFormatVersion;
	}


	public void setDigestFormatVersion(String digestFormatVersion) {
		this.digestFormatVersion = digestFormatVersion;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ProjectClassification getClassification() {
		return classification;
	}

	public void setClassification(ProjectClassification classification) {
		this.classification = classification;
	}

	public List<MDAbstractProject> getAllSortedProjects() {
		return allSortedProjects;
	}

	public void setAllSortedProjects(List<MDAbstractProject> allSortedProjects) {
		this.allSortedProjects = allSortedProjects;
	}

	public List<MDAbstractProjectUsage> getAllSortedUsages() {
		return allSortedUsages;
	}

	public void setAllSortedUsages(List<MDAbstractProjectUsage> allSortedUsages) {
		this.allSortedUsages = allSortedUsages;
	}

	public List<MDAbstractProject> getStronglyConnectedVertices() {
		return stronglyConnectedVertices;
	}

	public void setStronglyConnectedVertices(
			List<MDAbstractProject> stronglyConnectedVertices) {
		this.stronglyConnectedVertices = stronglyConnectedVertices;
	}

	public List<MDAbstractProjectUsage> getStronglyConnectedEdges() {
		return stronglyConnectedEdges;
	}

	public void setStronglyConnectedEdges(
			List<MDAbstractProjectUsage> stronglyConnectedEdges) {
		this.stronglyConnectedEdges = stronglyConnectedEdges;
	}

	public List<MDAbstractProject> getInconsistentlyUsedVertices() {
		return inconsistentlyUsedVertices;
	}

	public void setInconsistentlyUsedVertices(
			List<MDAbstractProject> inconsistentlyUsedVertices) {
		this.inconsistentlyUsedVertices = inconsistentlyUsedVertices;
	}

	public List<MDAbstractProjectUsage> getInconsistentUsageEdges() {
		return inconsistentUsageEdges;
	}

	public void setInconsistentUsageEdges(
			List<MDAbstractProjectUsage> inconsistentUsageEdges) {
		this.inconsistentUsageEdges = inconsistentUsageEdges;
	}

	public List<MDAbstractProjectUsage> getInvalidUsageEdges() {
		return invalidUsageEdges;
	}

	public void setInvalidUsageEdges(List<MDAbstractProjectUsage> invalidUsageEdges) {
		this.invalidUsageEdges = invalidUsageEdges;
	}

	public SortedMap<MDAbstractProject, List<MDAbstractProjectUsage>> getAllUsedByRelationships() {
		return allUsedByRelationships;
	}

	public void setAllUsedByRelationships(
			SortedMap<MDAbstractProject, List<MDAbstractProjectUsage>> allUsedByRelationships) {
		this.allUsedByRelationships = allUsedByRelationships;
	}

	public boolean isProjectMissingSystemOrStandardProfileFlag() {
		return isProjectMissingSystemOrStandardProfileFlag;
	}

	public void setProjectMissingSystemOrStandardProfileFlag(
			boolean isProjectMissingSystemOrStandardProfileFlag) {
		this.isProjectMissingSystemOrStandardProfileFlag = isProjectMissingSystemOrStandardProfileFlag;
	}

	public List<MDAbstractProject> getMissingProjects() {
		return missingProjects;
	}

	public void setMissingProjects(List<MDAbstractProject> missingProjects) {
		this.missingProjects = missingProjects;
	}

	public List<MDAbstractProject> getMissingDirectAttachments() {
		return missingDirectAttachments;
	}

	public void setMissingDirectAttachments(
			List<MDAbstractProject> missingDirectAttachments) {
		this.missingDirectAttachments = missingDirectAttachments;
	}

	public List<MDAbstractProject> getModulesWithMissingShares() {
		return modulesWithMissingShares;
	}

	public void setModulesWithMissingShares(
			List<MDAbstractProject> modulesWithMissingShares) {
		this.modulesWithMissingShares = modulesWithMissingShares;
	}

	public List<MDAbstractProjectUsage> getUnresolvedUsageEdges() {
		return unresolvedUsageEdges;
	}

	public void setUnresolvedUsageEdges(
			List<MDAbstractProjectUsage> unresolvedUsageEdges) {
		this.unresolvedUsageEdges = unresolvedUsageEdges;
	}

	public int getProxyCount() {
		return proxyCount;
	}

	public void setProxyCount(int proxyCount) {
		this.proxyCount = proxyCount;
	}

	public int getDiagramCount() {
		return diagramCount;
	}

	public void setDiagramCount(int diagramCount) {
		this.diagramCount = diagramCount;
	}

	public List<RecoveredElementProxy> getRecoveredElementProxies() {
		return recoveredElementProxies;
	}

	public void setRecoveredElementProxies(
			List<RecoveredElementProxy> recoveredElementProxies) {
		this.recoveredElementProxies = recoveredElementProxies;
	}
	
	public void addRecoveredElementProxy(final Element owner, final SortedSet<Element> ownedProxies) {
		RecoveredElementProxy rep = new RecoveredElementProxy();
		rep.setHumanName(owner.getHumanName());
		rep.setHumanType(owner.getHumanType());
		rep.setID(owner.getID());
		rep.setOwnedProxyCount(ownedProxies.size());
		this.getRecoveredElementProxies().add(rep);
	}

	
	public List<ElementProxyInfo> getGhostElementProxies() {
		return ghostElementProxies;
	}

	public void setGhostElementProxies(List<ElementProxyInfo> ghostElementProxies) {
		this.ghostElementProxies = ghostElementProxies;
	}

	public void addGhostElementProxy(final Element proxy) {
		this.getGhostElementProxies().add(new ElementProxyInfo(proxy));
	}

	public SortedMap<MDAbstractProject, List<ElementProxyInfo>> getMissingElementsInUnloadedModules() {
		return missingElementsInUnloadedModules;
	}

	public void setMissingElementsInUnloadedModules(
			SortedMap<MDAbstractProject, List<ElementProxyInfo>> missingElementsInUnloadedModules) {
		this.missingElementsInUnloadedModules = missingElementsInUnloadedModules;
	}
	
	public void addMissingElementsInUnloadedModule(final MDAbstractProject unloadedModule, final SortedSet<Element> unloadedProxies) {
		List<ElementProxyInfo> unloadedProxyInfo = new ArrayList<ElementProxyInfo>();
		for (Element proxy : unloadedProxies) {
			unloadedProxyInfo.add(new ElementProxyInfo(proxy));
		}
		getMissingElementsInUnloadedModules().put(unloadedModule, unloadedProxyInfo);
	}

	public List<ElementProxyInfo> getOtherMissingProxies() {
		return otherMissingProxies;
	}

	public void setOtherMissingProxies(List<ElementProxyInfo> otherMissingProxies) {
		this.otherMissingProxies = otherMissingProxies;
	}

	public List<DiagramProxyUsageProblems> getDiagramProxyUsageProblems() {
		return diagramProxyUsageProblems;
	}

	public void setDiagramProxyUsageProblems(
			List<DiagramProxyUsageProblems> diagramProxyUsageProblems) {
		this.diagramProxyUsageProblems = diagramProxyUsageProblems;
	}

	public List<MDAbstractProject> getShouldBeSystemOrStandardProfile() {
		return shouldBeSystemOrStandardProfile;
	}

	public void setShouldBeSystemOrStandardProfile(
			List<MDAbstractProject> shouldBeSystemOrStandardProfile) {
		this.shouldBeSystemOrStandardProfile = shouldBeSystemOrStandardProfile;
	}

	@Override
	public String toString() {
		StringBuffer gSerialization = new StringBuffer();
		
		gSerialization.append(String.format("'%s'\nSSCAEProjectUsageGraph(Vertices=%d, Edges=%d)", 
				getName(),
				getAllSortedProjects().size(),
				getAllSortedUsages().size()));

		gSerialization.append("\n --- projects ---");
		for (MDAbstractProject aProject : getAllSortedProjects()) {
			gSerialization.append(String.format("\n%s", aProject));
		}
		
		gSerialization.append("\n --- usedBy relationships ---");
		for (MDAbstractProject p2 : getAllSortedProjects()) {
			gSerialization.append(String.format("\n%s", p2.getIndex()));
			List<MDAbstractProjectUsage> p2UsedBy = getAllUsedByRelationships().get(p2);
			if (null != p2UsedBy)
				for (MDAbstractProjectUsage p1p2Usage : p2UsedBy) {
					gSerialization.append(String.format("\n - used by %s %s %s",
							p1p2Usage.getSource().getIndex(),
							p1p2Usage.getSignature(),
							p1p2Usage.getUsageConsistencyLabel()));
				}
		}

		gSerialization.append("\n --- validation results ---");
		if (isProjectMissingSystemOrStandardProfileFlag()) {
			gSerialization.append(String.format("\nERROR: this project should have the System/Standard Profile flag set"));
		}

		if (getMissingDirectAttachments().isEmpty()) {
			gSerialization.append(String.format("\n   OK: no missing direct ProjectUsage mount attachments"));
		} else {
			gSerialization.append(String.format("\nERROR: this project is missing %d direct ProjectUsage mount attachments", getMissingDirectAttachments().size()));
			for (MDAbstractProject missingV : getMissingDirectAttachments()) {
				gSerialization.append(String.format("\n - %s", missingV.getName()));
			}
		}

		if (!getUnresolvedUsageEdges().isEmpty()) {
			gSerialization.append(String.format("\nERROR: there are %d unresolved ProjectUsage relationships -- *** please save/commit and re-open the project to force resolution ***", getUnresolvedUsageEdges().size()));
			for (MDAbstractProjectUsage unresolvedUsage : getUnresolvedUsageEdges()) {
				gSerialization.append(String.format("\n - %s", unresolvedUsage));
			}
		}
	
		if (getProxyCount() == 0) {
			gSerialization.append(String.format("\n   OK: no proxies detected"));
		} else {
			gSerialization.append(String.format("\nERROR: %s proxies detected", getProxyCount()));	

			if (getRecoveredElementProxies().isEmpty())
				gSerialization.append(String.format("\n   OK: - no proxies for recovered elements"));
			else {
				gSerialization.append(String.format("\nERROR: - %s proxies for recovered elements", getRecoveredElementProxies().size()));
				for (RecoveredElementProxy rep : getRecoveredElementProxies()) {
					gSerialization.append(String.format("\n - '%s' : %s {ID=%s} has %d nested proxies",
							rep.getHumanName(), rep.getHumanType(), rep.getID(), rep.getOwnedProxyCount()));
				}
			}
			if (!getGhostElementProxies().isEmpty()) {
				gSerialization.append(String.format("\nERROR: - %s proxies for non-recovered elements (*** Notify SSCAE ***)", getGhostElementProxies().size()));
				for (ElementProxyInfo proxy : getGhostElementProxies()) {
					gSerialization.append(String.format("\n - '%s' : %s {ID=%s}",
							proxy.getHumanName(), proxy.getHumanType(), proxy.getID()));
				}
			}

			if (getMissingElementsInUnloadedModules().isEmpty())
				gSerialization.append(String.format("\n   OK: - no proxies for missing elements in loadable modules"));
			else {
				gSerialization.append(String.format("\nERROR: - %s proxies for missing elements in loadable modules", getMissingElementsInUnloadedModules().size()));
				for (Map.Entry<MDAbstractProject, List<ElementProxyInfo>> entry : getMissingElementsInUnloadedModules().entrySet()) {
					MDAbstractProject unloadedProject = entry.getKey();
					List<ElementProxyInfo> unloadedProxies = entry.getValue();
					gSerialization.append(String.format("\n - unloaded module '%s' {ID=%s}",
							unloadedProject.getName(), unloadedProject.getProjectID()));
					for (ElementProxyInfo proxy : unloadedProxies) {
						gSerialization.append(String.format("\n -- '%s' : %s {ID=%s}",
								proxy.getHumanName(), proxy.getHumanType(), proxy.getID()));
					}
				}
			}
			if (!getOtherMissingProxies().isEmpty()) {
				gSerialization.append(String.format("\nERROR: - %s proxies for missing elements elsewhere (*** Notify SSCAE ***)", getOtherMissingProxies().size()));
				for (ElementProxyInfo proxy : getOtherMissingProxies()) {
					gSerialization.append(String.format("\n - '%s' : %s {ID=%s}",
							proxy.getHumanName(), proxy.getHumanType(), proxy.getID()));
				}
			}

			if (getDiagramProxyUsageProblems().isEmpty()) {
				gSerialization.append(String.format("\n   OK: none of the %d diagrams have proxy usage problems", getDiagramCount()));
			} else {
				gSerialization.append(String.format("\nERROR: %d / %d diagrams have proxy usage problems", 
						getDiagramProxyUsageProblems().size(), getDiagramCount()));
				for(DiagramProxyUsageProblems d : getDiagramProxyUsageProblems()) {
					gSerialization.append(String.format("\n - diagram '%s' {ID=%s, type=%s} uses %d proxy elements",
							d.getQualifiedName(), d.getID(), d.getType(), d.getProxyCount()));
				}
			}
		}

		if (getMissingProjects().isEmpty()) {
			gSerialization.append(String.format("\n   OK: all projects are available"));
		} else {
			gSerialization.append(String.format("\nERROR: %d projects are missing",
					getMissingProjects().size()));
			for(MDAbstractProject v : getMissingProjects()) {
				gSerialization.append("\n missing: " + v.getIndex());
			}
		}
		
		if (getModulesWithMissingShares().isEmpty()) {
			gSerialization.append(String.format("\n   OK: all projects have no missing shares"));
		} else {
			gSerialization.append(String.format("\nERROR: %d projects have missing shares",
					getModulesWithMissingShares().size()));
			for (MDAbstractProject v : getModulesWithMissingShares()) {
				gSerialization.append("\n module with missing share: " + v.getIndex());
			}
		}
		
		if (getShouldBeSystemOrStandardProfile().isEmpty()) {
			gSerialization.append(String.format("\n   OK: all local projects used from MD's install folder have the Standard/System Profile flag set"));
		} else {
			gSerialization.append(String.format("\nERROR: %d projects used from MD's install folder do not have the Standard/System Profile (SSP) flag set",
					getShouldBeSystemOrStandardProfile().size()));
			for (MDAbstractProject v : getShouldBeSystemOrStandardProfile()) {
				gSerialization.append("\n incorrect SSP flag: " + v.getIndex());
			}
		}
		
		if (getStronglyConnectedEdges().isEmpty()) {
			gSerialization.append(String.format("\n   OK: project usage mount relationships are acyclic"));
		} else {
			gSerialization.append(String.format("\nERROR: %d projects are involved in %d project usage mount cyclic relationships", 
					getStronglyConnectedVertices().size(),
					getStronglyConnectedEdges().size()));
			for (MDAbstractProject v : getStronglyConnectedVertices()) {
				gSerialization.append("\n cyclic vertex: " + v.getIndex());
			}
			gSerialization.append("\n --- cyclic edges ---");
			for (MDAbstractProjectUsage e : getStronglyConnectedEdges()) {
				gSerialization.append("\n cyclic edge: " + e.getIndex());
			}
		}

		if (getInconsistentUsageEdges().isEmpty()) {
			gSerialization.append(String.format("\n   OK: project usage mount relationships are consistent"));
		} else {
			gSerialization.append(String.format("\nERROR: %d project usage mount relationships are inconsistent", getInconsistentUsageEdges().size()));
			for (MDAbstractProjectUsage e : getInconsistentUsageEdges()) {
				gSerialization.append("\n inconsistent edge: " + e.getIndex());
			}
		}

		if (getInconsistentlyUsedVertices().isEmpty()) {
			gSerialization.append(String.format("\n   OK: all projects are used consistently"));
		} else {
			gSerialization.append(String.format("\nERROR: %d projects are used inconsistently", getInconsistentlyUsedVertices().size()));
			for (MDAbstractProject v : getInconsistentlyUsedVertices()) {
				gSerialization.append("\n inconsistently used: " + v.getIndex());
			}
		}

		if (getInvalidUsageEdges().isEmpty()) {
			gSerialization.append(String.format("\n   OK: project usage mount relationships are valid"));
		} else {
			gSerialization.append(String.format("\nERROR: %d project usage mount relationships are invalid",  getInvalidUsageEdges().size()));
			for (MDAbstractProjectUsage e : getInvalidUsageEdges()) {
				gSerialization.append("\n invalid edge: " + e.getIndex());
			}
		}
		
		return gSerialization.toString();
	}
}