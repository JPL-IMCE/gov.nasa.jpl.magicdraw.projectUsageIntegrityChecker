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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation;
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

import gov.nasa.jpl.logfire.RunnableSessionWrapperWithResult;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph;

import java.lang.Class;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.validation.ElementValidationRuleImpl;
import com.nomagic.magicdraw.validation.SmartListenerConfigurationProvider;
import com.nomagic.magicdraw.validation.ValidationSuiteHelper;
import com.nomagic.uml2.ext.jmi.smartlistener.SmartListenerConfig;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

/**
 * @author Nicolas F. Rouquette (JPL)
 * @author Alek Kerzhner (JPL)
 * 
 * This validation rule applies to a project's toplevel model only.
 * If the project is invalid, the annotation error will show the project usage graph's diagnostics.
 */
public class SSCAEValidProjectUsageGraphValidation 
implements ElementValidationRuleImpl, SmartListenerConfigurationProvider {

	public SSCAEValidProjectUsageGraphValidation() {}

	/**
	 * Trigger validation when creating a kind of package (i.e., model, package profile)
	 */
	@Override
	public Map<Class<? extends Element>, Collection<SmartListenerConfig>> getListenerConfigurations() {
		Map<Class<? extends Element>, Collection<SmartListenerConfig>> configMap = new HashMap<Class<? extends Element>, Collection<SmartListenerConfig>>();
		Collection<SmartListenerConfig> configs = new ArrayList<SmartListenerConfig>();
		configs.add(SmartListenerConfig.NAME_CONFIG);
		configMap.put(com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model.class, configs);
		configMap.put(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package.class, configs);
		configMap.put(com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile.class, configs);
		return configMap;
	}

	@Override
	public void init(Project p, Constraint c) {
	}

	@Override
	public Set<Annotation> run(final Project project, final Constraint constraint, final Collection<? extends Element> elements) {
		ProjectsManager manager = Application.getInstance().getProjectsManager();
		if (!manager.isProjectActive(project))
			return Collections.emptySet();

		final String sessionLabel = String.format("SSCAEValidProjectUsageGraphValidation('%s')", project.getName());
		return new RunnableSessionWrapperWithResult<Set<Annotation>>(sessionLabel) {

			@Override
			protected Set<Annotation> run() {			
				ProjectUsageIntegrityHelper usageHelper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(project);
				if (null == usageHelper) 
					return Collections.emptySet();

				SSCAEProjectUsageGraph graph = usageHelper.latestProjectUsageGraph;
				if (null == graph)
					return Collections.emptySet();

				if (graph.stronglyConnectedEdges.isEmpty())
					return Collections.emptySet();

				Set<Annotation> annotations = new HashSet<Annotation>();

				for (Element element : elements) {
					if (element instanceof Model) {
						Model m = (Model) element;
						if (m.equals(project.getModel())) {
							EnumerationLiteral errorLevel = ValidationSuiteHelper.getInstance(project).getSeverityLevel("error");
							Annotation a = new Annotation(errorLevel, "SSCAE", graph.getProjectUsageGraphDiagnostic(), m);
							annotations.add(a);
						}
					}
				}
				
				log.info(String.format("%s - %d annotations", sessionLabel, annotations.size()));
				return annotations;						
			}
		}.execute();
	}

	@Override
	public void dispose() {
	}

}