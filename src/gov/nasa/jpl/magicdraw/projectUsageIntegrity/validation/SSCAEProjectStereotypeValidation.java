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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.ci.persistence.IProject;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.validation.ElementValidationRuleImpl;
import com.nomagic.magicdraw.validation.SmartListenerConfigurationProvider;
import com.nomagic.uml2.ext.jmi.smartlistener.SmartListenerConfig;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

/**
 * @author Nicolas F. Rouquette (JPL)
 * @author Alek Kerzhner (JPL)
 * 
 * This validation rule applies to every shared package.
 * The intent is to verify that every shared package as the SSCAEProject stereotype applied.
 */
public class SSCAEProjectStereotypeValidation 
implements ElementValidationRuleImpl, SmartListenerConfigurationProvider {

	public SSCAEProjectStereotypeValidation() {}

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

		final String sessionLabel = String.format("SSCAEProjectStereotypeValidation('%s')", project.getName());
		return new RunnableSessionWrapperWithResult<Set<Annotation>>(sessionLabel) {

			@Override
			protected Set<Annotation> run() {		
				ProjectUsageIntegrityHelper usageHelper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(project);
				if (null == usageHelper) 
					return Collections.emptySet();

				if (null == usageHelper.latestProjectUsageGraph)
					return Collections.emptySet();
					
				if (null == usageHelper.getSharedPackageStereotype())
					return Collections.emptySet();

				IPrimaryProject pp = project.getPrimaryProject();
				Collection<Package> sharedPackages = ProjectUtilities.getSharedPackages(pp);

				Set<Annotation> annotations = new HashSet<Annotation>();

				for (Element e : elements) {
					if (e instanceof Package) {
						Package p = (Package) e;

						// does p belong to the project?
						IProject ip = ProjectUtilities.getProject(p);
						if (! pp.equals(ip)) continue;

						if (sharedPackages.contains(p)) {

							// if p is a shared package, then it should have <<SSCAE Project>> applied.
							if (!usageHelper.hasSSCAESharedPackageStereotypeApplied(p)) {
								annotations.add(usageHelper.createSSCAEStereotypeApplicationAnnotation(p, constraint, usageHelper.getSharedPackageStereotype()));
							}

						} else {

							// if p is not a shared package, then it should not have <<SSCAE Project>> applied.
							if (usageHelper.hasSSCAESharedPackageStereotypeApplied(p)) {
								annotations.add(usageHelper.createSSCAEStereotypeUnapplicationAnnotation(p, constraint, usageHelper.getSharedPackageStereotype()));
							}

						}
					}
				}

				Model root = project.getModel();
				if (root != null){
					if (!usageHelper.hasSSCAEProjectModelStereotypeApplied(root)){
						annotations.add(usageHelper.createSSCAEStereotypeApplicationAnnotation(root, constraint, usageHelper.getProjectModelStereotype()));
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
