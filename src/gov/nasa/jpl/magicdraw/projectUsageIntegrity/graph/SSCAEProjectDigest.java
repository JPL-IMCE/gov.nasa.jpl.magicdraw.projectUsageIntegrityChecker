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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.expression.ElementPath;

import java.lang.String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.eclipse.emf.common.util.URI;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.actions.SelectInContainmentTreeRunnable;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

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
	private List<ProfileNameConflict> userProfileNameConflicts;
	private List<ProfileNameConflict> sspProfileNameConflicts;
	private List<URIConflict> packageURIConflicts;
	private List<URIConflict> profileURIConflicts;


	public void dispose() {
		classification = ProjectClassification.INVALID;
		proxyCount = 0;
		diagramCount = 0;
		if (allSortedProjects != null) {
			allSortedProjects.clear();
			allSortedProjects = null;
		}
		if (allSortedUsages != null) {
			allSortedUsages.clear();
			allSortedUsages = null;
		}
		if (stronglyConnectedVertices != null) {
			stronglyConnectedVertices.clear();
			stronglyConnectedVertices = null;
		}
		if (stronglyConnectedEdges != null) {
			stronglyConnectedEdges.clear();
			stronglyConnectedEdges = null;
		}
		if (inconsistentlyUsedVertices != null) {
			inconsistentlyUsedVertices.clear();
			inconsistentlyUsedVertices = null;
		}
		if (inconsistentUsageEdges != null) {
			inconsistentUsageEdges.clear();
			inconsistentUsageEdges = null;
		}
		if (invalidUsageEdges != null) {
			invalidUsageEdges.clear();
			invalidUsageEdges = null;
		}
		if (allUsedByRelationships != null) {
			allUsedByRelationships.clear();
			allUsedByRelationships = null;
		}
		if (userProfileNameConflicts != null) {
			userProfileNameConflicts.clear();
			userProfileNameConflicts = null;
		}
		if (sspProfileNameConflicts != null) {
			sspProfileNameConflicts.clear();
			sspProfileNameConflicts = null;
		}
		if (packageURIConflicts != null) {
			packageURIConflicts.clear();
			packageURIConflicts = null;
		}
		if (profileURIConflicts != null) {
			profileURIConflicts.clear();
			profileURIConflicts = null;
		}
	}

	/**
	 * should be false
	 */
	private boolean isProjectMissingSystemOrStandardProfileFlag;

	/**
	 * should be empty
	 */
	private List<MDAbstractProject> missingProjects;

	public List<ProfileNameConflict> getUserProfileNameConflicts() {
		return userProfileNameConflicts;
	}

	public void setUserProfileNameConflicts(
			List<ProfileNameConflict> profileNameConflicts) {
		this.userProfileNameConflicts = profileNameConflicts;
	}

	public List<ProfileNameConflict> getSSPProfileNameConflicts() {
		return sspProfileNameConflicts;
	}

	public void setSSPProfileNameConflicts(
			List<ProfileNameConflict> profileNameConflicts) {
		this.sspProfileNameConflicts = profileNameConflicts;
	}


	public List<URIConflict> getPackageURIConflicts() {
		return packageURIConflicts;
	}


	public void setPackageURIConflicts(List<URIConflict> packageURIConflicts) {
		this.packageURIConflicts = packageURIConflicts;
	}


	public List<URIConflict> getProfileURIConflicts() {
		return profileURIConflicts;
	}


	public void setProfileURIConflicts(List<URIConflict> profileURIConflicts) {
		this.profileURIConflicts = profileURIConflicts;
	}

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
		this.setUserProfileNameConflicts(new ArrayList<ProfileNameConflict>());
		this.setSSPProfileNameConflicts(new ArrayList<ProfileNameConflict>());
		this.setPackageURIConflicts(new ArrayList<URIConflict>());
		this.setProfileURIConflicts(new ArrayList<URIConflict>());
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
		rep.setElementPath(ElementPath.getElementName(owner));
		List<ElementProxyInfo> proxies = new ArrayList<ElementProxyInfo>();
		for (Element ownedProxy : ownedProxies) {
			proxies.add(new ElementProxyInfo(owner, ownedProxy));
		}
		rep.setOwnedElementProxies(proxies);
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

		if (getRecoveredElementProxies().isEmpty())
			gSerialization.append(String.format("\n   OK: - no proxies for recovered elements"));
		else {
			gSerialization.append(String.format("\nERROR: - %s proxies for recovered elements", getRecoveredElementProxies().size()));
			for (RecoveredElementProxy rep : getRecoveredElementProxies()) {
				gSerialization.append(String.format("\n - %s has %d nested proxies",
						rep.getElementPath(), rep.getOwnedProxyCount()));
				for (ElementProxyInfo proxy : rep.getOwnedElementProxies()) {
					gSerialization.append(String.format("\n -- %s",
							proxy.getElementPath()));
				}
			}
		}
		if (!getGhostElementProxies().isEmpty()) {
			gSerialization.append(String.format("\nERROR: - %s proxies for non-recovered elements (*** Notify SSCAE ***)", getGhostElementProxies().size()));
			for (ElementProxyInfo proxy : getGhostElementProxies()) {
				gSerialization.append(String.format("\n -- %s",
						proxy.getElementPath()));
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
					gSerialization.append(String.format("\n -- %s",
							proxy.getElementPath()));
				}
			}
		}
		if (!getOtherMissingProxies().isEmpty()) {
			gSerialization.append(String.format("\nERROR: - %s proxies for missing elements elsewhere (*** Notify SSCAE ***)", getOtherMissingProxies().size()));
			for (ElementProxyInfo proxy : getOtherMissingProxies()) {
				gSerialization.append(String.format("\n -- %s",
						proxy.getElementPath()));
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

	public static String FIRST_SEGMENT = "=> <A>%s</A>\n";
	public static String OTHER_SEGMENTS = " . <A>%s</A>\n";

	public void showProblems(SSCAEProjectUsageGraph g) {
		final Application a = Application.getInstance();
		final Project project = a.getProject();
		final URI location = project.getLocationUri();
		final GUILog log = a.getGUILog();

		log.log(String.format("= Project: %s ==========================================\n", ((location == null) ? project.getName() : location)));

		if (!getRecoveredElementProxies().isEmpty()) {
			log.log(String.format("ERROR: - %s proxies for recovered elements\n", getRecoveredElementProxies().size()));

			if (getRecoveredElementProxies().size() < 10){

				for (RecoveredElementProxy rep : getRecoveredElementProxies()) {
					log.log(String.format("- %d nested proxies in %s\n", rep.getOwnedProxyCount(), rep.getElementPath()));
					if ( rep.getOwnedElementProxies().size() < 10){
						for (ElementProxyInfo proxy : rep.getOwnedElementProxies()) {
							ElementPath.showElementPath(project, log, FIRST_SEGMENT, OTHER_SEGMENTS, proxy.getElementPath());
						}
					}
				}
			}
		}

		if (!getGhostElementProxies().isEmpty()) {
			log.log(String.format("ERROR: - %s proxies for non-recovered elements (*** Notify SSCAE ***)\n", getGhostElementProxies().size()));
			if (getGhostElementProxies().size() < 10){

				for (ElementProxyInfo proxy : getGhostElementProxies()) {
					ElementPath.showElementPath(project, log, FIRST_SEGMENT, OTHER_SEGMENTS, proxy.getElementPath());
				}
			}
		}

		if (!getMissingElementsInUnloadedModules().isEmpty()) {
			log.log(String.format("ERROR: - %s proxies for missing elements in loadable modules\n", getMissingElementsInUnloadedModules().size()));
			if (getGhostElementProxies().size() < 10){

				for (Map.Entry<MDAbstractProject, List<ElementProxyInfo>> entry : getMissingElementsInUnloadedModules().entrySet()) {
					MDAbstractProject unloadedProject = entry.getKey();
					List<ElementProxyInfo> unloadedProxies = entry.getValue();
					log.log(String.format("- unloaded module '%s' {ID=%s}\n", unloadedProject.getName(), unloadedProject.getProjectID()));
					if (unloadedProxies.size() < 10){

						for (ElementProxyInfo proxy : unloadedProxies) {
							ElementPath.showElementPath(project, log, FIRST_SEGMENT, OTHER_SEGMENTS, proxy.getElementPath());
						}
					}
				}
			}
		}

		if (!getOtherMissingProxies().isEmpty()) {
			log.log(String.format("ERROR: - %s proxies for missing elements elsewhere (*** Notify SSCAE ***)\n", getOtherMissingProxies().size()));
			if (getOtherMissingProxies().size() < 10){

				for (ElementProxyInfo proxy : getOtherMissingProxies()) {
					ElementPath.showElementPath(project, log, FIRST_SEGMENT, OTHER_SEGMENTS, proxy.getElementPath());
				}
			}
		}

		if (!getDiagramProxyUsageProblems().isEmpty()) {
			log.log(String.format("ERROR: %d / %d diagrams have proxy usage problems\n", 
					getDiagramProxyUsageProblems().size(), getDiagramCount()));
			if (getDiagramProxyUsageProblems().size() < 10){
				for(DiagramProxyUsageProblems d : getDiagramProxyUsageProblems()) {
					log.addHyperlinkedText(
							String.format("- diagram '<A>%s</A>' {ID=%s, type=%s} uses %d proxy elements\n", d.getQualifiedName(), d.getID(), d.getType(), d.getProxyCount()), 
							Collections.singletonMap(d.getID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(d.getID()))));
				}
			}
		}

		if (!getPackageURIConflicts().isEmpty()) {
			log.log(String.format("ERROR: %d package URI conflicts\n", getPackageURIConflicts().size()));
			for (URIConflict conflict : getPackageURIConflicts()) {
				log.log(String.format("Package URI conflict: %s\n", conflict.getURI()));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP1()), 
						Collections.singletonMap(conflict.getP1ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP1ID()))));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP2()), 
						Collections.singletonMap(conflict.getP2ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP2ID()))));
			}
		}


		if (!getProfileURIConflicts().isEmpty()) {
			log.log(String.format("ERROR: %d profile URI conflicts\n", getProfileURIConflicts().size()));
			for (URIConflict conflict : getProfileURIConflicts()) {
				log.log(String.format("Profile URI conflict: %s\n", conflict.getURI()));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP1()), 
						Collections.singletonMap(conflict.getP1ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP1ID()))));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP2()), 
						Collections.singletonMap(conflict.getP2ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP2ID()))));
			}
		}

		if (!getUserProfileNameConflicts().isEmpty()) {
			log.log(String.format("ERROR: %d user profile name conflicts\n", getUserProfileNameConflicts().size()));
			for (ProfileNameConflict conflict : getUserProfileNameConflicts()) {
				log.log(String.format("Profile name conflict:\n"));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP1()), 
						Collections.singletonMap(conflict.getP1ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP1ID()))));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP2()), 
						Collections.singletonMap(conflict.getP2ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP2ID()))));
			}
		}

		if (!getSSPProfileNameConflicts().isEmpty()) {
			log.log(String.format("WARNING: %d S/SP profile name conflicts\n", getSSPProfileNameConflicts().size()));
			for (ProfileNameConflict conflict : getSSPProfileNameConflicts()) {
				log.log(String.format("Profile name conflict:\n"));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP1()), 
						Collections.singletonMap(conflict.getP1ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP1ID()))));
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", conflict.getP2()), 
						Collections.singletonMap(conflict.getP2ID(), (Runnable) new SelectInContainmentTreeRunnable(project.getElementByID(conflict.getP2ID()))));
			}
		}

		if (!g.noSharedPackage_constrainedAs_WARNING_fromUsages) {
			List<Package> warnings = new ArrayList<Package>();
			for (Package p : g.sharedPackages_constrainedAs_WARNING_fromUsages.keySet()) {
				if (!g.sharedPackages_constrainedAs_WARNING_fromUsages.get(p).isEmpty())
					warnings.add(p);
			}
			log.log(String.format(" WARN: %d shared packages have some usage constraint classified as WARNING\n",  warnings.size()));
			for (Package p : warnings) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.noSharedPackage_constrainedAs_ERROR_fromUsages) {
			List<Package> errors = new ArrayList<Package>();
			for (Package p : g.sharedPackages_constrainedAs_ERROR_fromUsages.keySet()) {
				if (!g.sharedPackages_constrainedAs_ERROR_fromUsages.get(p).isEmpty())
					errors.add(p);
			}
			log.log(String.format("ERROR: %d shared packages have some usage constraint classified as ERROR\n",  errors.size()));
			for (Package p : errors) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.no_DEPRECATED_WARNING_constraintViolations) {
			log.log(String.format(" WARN: %d DEPRECATED shared packages have some usage constraint classified as WARNING", g.sharedPackages_classified_DEPRECATED.size()));
			for (Package p : g.sharedPackages_classified_DEPRECATED) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.no_DEPRECATED_ERROR_constraintViolations) {
			log.log(String.format("ERROR: %d DEPRECATED shared packages have some usage constraint classified as ERROR", g.sharedPackages_classified_DEPRECATED.size()));
			for (Package p : g.sharedPackages_classified_DEPRECATED) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.no_INCUBATOR_WARNING_constraintViolations) {
			log.log(String.format(" WARN: %d INCUBATOR shared packages have some usage constraint classified as WARNING", g.sharedPackages_classified_INCUBATOR.size()));
			for (Package p : g.sharedPackages_classified_INCUBATOR) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.no_INCUBATOR_ERROR_constraintViolations) {
			log.log(String.format("ERROR: %d INCUBATOR shared packages have some usage constraint classified as ERROR", g.sharedPackages_classified_INCUBATOR.size()));
			for (Package p : g.sharedPackages_classified_INCUBATOR) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.no_RECOMMENDED_WARNING_constraintViolations) {
			log.log(String.format(" WARN: %d RECOMMENDED shared packages have some usage constraint classified as WARNING", g.sharedPackages_classified_RECOMMENDED.size()));
			for (Package p : g.sharedPackages_classified_RECOMMENDED) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.no_RECOMMENDED_ERROR_constraintViolations) {
			log.log(String.format("ERROR: %d RECOMMENDED shared packages have some usage constraint classified as ERROR", g.sharedPackages_classified_RECOMMENDED.size()));
			for (Package p : g.sharedPackages_classified_RECOMMENDED) {
				log.addHyperlinkedText(
						String.format("=> <A>%s</A>\n", p.getQualifiedName()), 
						Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));
			}
		}

		if (!g.moduleOrProjectWithInconsistentlyClassifiedSharedPackages.isEmpty()) {
			log.log(String.format("ERROR: %d modules/project with INCONSISTENT shared packages classifications", g.moduleOrProjectWithInconsistentlyClassifiedSharedPackages.size()));
			for (MDAbstractProject v : g.moduleOrProjectWithInconsistentlyClassifiedSharedPackages) {
				for (Package p : g.moduleOrProject2SharedPackages.get(v)) {
					log.addHyperlinkedText(
							String.format("=> <A>%s</A> is inconsistently classified within module/project '%s'\n", 
									p.getQualifiedName(), v.getName()), 
									Collections.singletonMap(p.getQualifiedName(), (Runnable) new SelectInContainmentTreeRunnable(p)));					
				}
			}	
		}

		if (!g.missingSSPProjects.isEmpty()){

			for (MDAbstractProject missingProject : g.missingSSPProjects) {
				log.log(String.format("\n project missing SSP flag: %s", missingProject.getName()));
			}
		}


		log.log(String.format("===========================================\n"));
	}

}