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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.validation.ElementValidationRuleImpl;
import com.nomagic.magicdraw.validation.SmartListenerConfigurationProvider;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.jmi.smartlistener.SmartListenerConfig;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;

/**
 * @author Nicolas F. Rouquette (JPL)
 * @author Alek Kerzhner (JPL)
 * 
 * This validation rule applies to every profile
 * Checks uniqueness constraints
 * 
 */
public class SSCAEProfileValidation 
implements ElementValidationRuleImpl, SmartListenerConfigurationProvider {

	public SSCAEProfileValidation() {}

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

		final String sessionLabel = String.format("SSCAEProfileValidation('%s')", project.getName());
		return new RunnableSessionWrapperWithResult<Set<Annotation>>(sessionLabel) {

			@Override
			protected Set<Annotation> run() {		
				ProjectUsageIntegrityHelper usageHelper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(project);
				if (null == usageHelper) 
					return Collections.emptySet();

				if (null == usageHelper.latestProjectUsageGraph)
					return Collections.emptySet();

				Set<Annotation> annotations = new HashSet<Annotation>();

				for (Element e : elements) {
					if (e instanceof Profile) {
						Profile p = (Profile) e;


						List <Profile> projectProfiles = StereotypesHelper.getAllProfiles(project);

						if (projectProfiles.isEmpty()){
							return Collections.emptySet();
						}							
						
						for (Profile profile : projectProfiles){
							if (p.equals(profile))
								continue;
							
							if (p.getName().equals(profile.getName())){
								annotations.add(new Annotation(usageHelper.getValidationErrorLevel(), 
										String.format("SSCAE %s has non-unique name", p.getName()), 
										String.format("SSCAE %s has non-unique name: conflict with %s", p.getQualifiedName(), profile.getQualifiedName()),
										p));								
							}
							
							if (!p.getURI().equals("") && !profile.getURI().equals("")){
								if (p.getURI().equals(profile.getURI())){
									annotations.add(new Annotation(usageHelper.getValidationErrorLevel(), 
											String.format("SSCAE %s has non-unique URI", p.getName()), 
											String.format("SSCAE %s has non-unique URI: conflict with %s", p.getQualifiedName(), profile.getQualifiedName()),
											p));	
								} else if (profile.getURI().length() <= p.getURI().length()){
									if (appendSeparator(p.getURI()).startsWith(appendSeparator(profile.getURI()))){
										annotations.add(new Annotation(usageHelper.getValidationErrorLevel(), 
												String.format("SSCAE %s has non-unique URI", p.getName()), 
												String.format("SSCAE %s has non-unique URI: %s's URI is a substring", p.getQualifiedName(), profile.getQualifiedName()),
												p));	
									}
								} else {
									if (appendSeparator(profile.getURI()).startsWith(appendSeparator(p.getURI()))){
										annotations.add(new Annotation(usageHelper.getValidationErrorLevel(), 
												String.format("SSCAE %s has non-unique URI", p.getName()), 
												String.format("SSCAE %s has non-unique URI: substring of %s's URI", p.getQualifiedName(), profile.getQualifiedName()),
												p));		
									}
								}
							}
							
							
						}

					}
				}

				log.info(String.format("%s - %d annotations", sessionLabel, annotations.size()));
				return annotations;
			}
		}.execute();
	}
	
	public static String appendSeparator(String URI){
		if (!URI.endsWith("/")){
			URI = URI.concat("/");
			
		}
		return URI;
	}

	@Override
	public void dispose() {
	}

}
