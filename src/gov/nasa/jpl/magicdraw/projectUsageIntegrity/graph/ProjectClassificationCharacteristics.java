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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.ci.persistence.local.ProjectConfigurationException;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.proxy.ProxyManager;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

/**
 * From Vygantas Gedgaudas
 * @see https://support.nomagic.com/browse/MDUMLCS-9214?focusedCommentId=52375#action_52375
 * 
 * Nicolas: 
 * If the MD API had a way to tell us whether an IAttachedProject has private data, 
 * then I would use this to approve / reject operations that mount a module.
 * 
 * Vygantas:
 * There is not such API. We even haven't got such information stored (but we will have in 17.0.4 as far as I know) to support the API.
 * But I think that there is an easier way to do the math in case of IPrimaryProject. 
 * Collect all project's shared packages recursively via com.nomagic.magicdraw.core.ProjectUtilities.getSharedPackagesIncludingResharedRecursively(IProject). 
 * This utility collects all shared packages of the given project and also includes all shared packages of its re-shared modules. 
 * Then includes all shared packages of re-shared sub-modules, and so on.
 * That is, it performs a full traverse via re-shared usages. 
 * So, when you have the set of all shared packages, all you have to do is to analyze direct contents of the root element - "Data" (Model). 
 * 
 * If there is at least one element that does not belong to the previously collected set, 
 * then this means that the opened project has private data. I think that this information together with indication of 
 * whether the shared package set is empty or not is enough to determine whether the project is "project", "module", or "hybrid".
 * 
 * Important: when analyzing direct contents of the root element, ignore recovered elements that are not in the shared packages set. 
 * Elements that are recovered due to dependencies from shared part to private part inside a module may be mounted on the root element (Data). 
 * Although, some of such recovered elements could be there for a different reason: 
 * for example if some module remembers that this primary project had an element there and that the module depends on. 
 * I am not sure if you should ignore these.. Note, that such situation means a circular usage.
 */
public class ProjectClassificationCharacteristics {

	public final ProjectClassification projectClassification;
	
	/**
	 * Does the project have proxies for recovered elements (i.e., not found or deleted) ?
	 */
	public final boolean projectHasRecoveredProxies;
	
	/**
	 * Does the project have proxies for missing elements (i.e., unloaded) ?
	 */
	public final boolean projectHasMissingProxies;
	
	/**
	 * The shared packages of this project
	 */
	public final Collection<? extends Element> projectSharedPackages;
	
	/**
	 * This project's private elements
	 */
	public final Collection<? extends Element> projectPrivateData;

	/**
	 * @param p
	 * @throws ProjectConfigurationException
	 */
	public ProjectClassificationCharacteristics(@Nonnull Project p) throws ProjectConfigurationException {
		final IPrimaryProject pp = p.getPrimaryProject();
		Collection<? extends Element> projectSharedPackages = ProjectUtilities.getSharedPackages(pp);

		final ProxyManager proxyManager = p.getProxyManager();
		final Collection<? extends Element> allSharedPackages = ProjectUtilities.getSharedPackagesIncludingResharedRecursively(pp);
		final List<Element> privateElements = new ArrayList<Element>();

		boolean hasPrivateData = false;
		boolean hasRecoveredProxies = false;
		boolean hasMissingProxies = false;
		Model root = p.getModel();
		for (Element e : root.getOwnedElement()) {
			if (proxyManager.isGhostProxy(e)) {
				hasRecoveredProxies = true;
				continue;
			}
			if (proxyManager.isElementProxy(e)) {
				hasMissingProxies = true;
				continue;
			}
			boolean foundInSharedPackage = false;
			Element current;
			Element parent = e;
			do {
				current = parent;
				if (allSharedPackages.contains(current)) {
					foundInSharedPackage = true;
					break;
				}
				parent = ModelHelper.findParentOfType(current, Package.class);
			} while (parent != null && parent != current && !root.equals(parent));

			if (e instanceof Comment && root.equals(parent))
				continue;

			if (e instanceof InstanceSpecification && root.equals(parent) && e.equals(root.getAppliedStereotypeInstance()))
				continue;

			if (!foundInSharedPackage) {
				hasPrivateData = true;
				privateElements.add(e);
			}
		}

		this.projectSharedPackages = Collections.unmodifiableCollection(allSharedPackages);
		this.projectPrivateData = Collections.unmodifiableCollection(privateElements);

		for (Element e : proxyManager.getProxies()) {
			if (proxyManager.isGhostProxy(e)) {
				hasRecoveredProxies = true;
				continue;
			}
			hasMissingProxies = true;
		}

		this.projectHasRecoveredProxies = hasRecoveredProxies;
		this.projectHasMissingProxies = hasMissingProxies;

		// nothing is shared => this is some kind of project.
		if (projectSharedPackages.isEmpty()) {
			if (hasRecoveredProxies && !hasMissingProxies) {
				this.projectClassification = ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_RECOVERED_ELEMENTS;
			} else if (!hasRecoveredProxies && hasMissingProxies) {
				this.projectClassification = ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_ELEMENTS;
			} else if (hasRecoveredProxies && hasMissingProxies) {
				this.projectClassification = ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS;
			} else {
				this.projectClassification = ProjectClassification.IS_PROJECT;
			}
		} 
		else

			// some shared packages and some private data => this is some kind of project/module hybrid
			if (hasPrivateData) {
				if (hasRecoveredProxies && !hasMissingProxies) {
					this.projectClassification = ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS;
				} else if (!hasRecoveredProxies && hasMissingProxies) {
					this.projectClassification = ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS;
				} else if (hasRecoveredProxies && hasMissingProxies) {
					this.projectClassification = ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS;
				} else  {
					this.projectClassification = ProjectClassification.IS_HYBRID_PROJECT_MODULE;
				}
			}
			else

				// some shared packages and no private data => this is a some kind of module
				if (hasRecoveredProxies && !hasMissingProxies) {
					this.projectClassification =  ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS;
				} else if (!hasRecoveredProxies && hasMissingProxies) {
					this.projectClassification =  ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS;
				} else if (hasRecoveredProxies && hasMissingProxies) {
					this.projectClassification =  ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS;
				} else {
					this.projectClassification = ProjectClassification.IS_MODULE;
				}
	}

}