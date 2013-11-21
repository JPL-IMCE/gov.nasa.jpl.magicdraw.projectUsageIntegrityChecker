package gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions;
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

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.eclipse.emf.ecore.EObject;

import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.mounting.IMountPoint;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.modules.ModulesService;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class RepairUnloadedModuleWithProxiesAction extends AbstractRepairProjectUsageAction {

	private static final long serialVersionUID = -6527969457906756269L;

	protected final Project project;
	protected final IAttachedProject unloadedProject;
	protected final Set<Element> unloadedProxies;
	protected final String message;
	protected final Element context;
	
	public RepairUnloadedModuleWithProxiesAction(
			@Nonnull ProjectUsageIntegrityHelper helper, 
			@Nonnull Project project,
			@Nonnull IAttachedProject unloadedProject,
			@Nonnull Set<Element> unloadedProxies) {
		super("PROJECT_USAGE_INTEGRITY_CHECKER_REPAIR_UNLOADED_MODULE_WITH_PROXIES_ACTION", 
				String.format("Load module '%s' to resolve %d unloaded proxies", unloadedProject.getName(), unloadedProxies.size()), 
				0, helper);
		this.project = project;
		this.unloadedProject = unloadedProject;
		this.unloadedProxies = unloadedProxies;
		this.message = String.format("Unloaded module '%s' {ID=%s} has %d unloaded proxy elements", 
				unloadedProject.getName(), unloadedProject.getProjectID(), unloadedProxies.size());
		
		Set<Element> mountedSharePoints = new HashSet<Element>();
		IPrimaryProject pp = project.getPrimaryProject();
		Set<IMountPoint> mountPoints = ProjectUtilities.getMountPointsFor(pp, unloadedProject);
		for (IMountPoint mountPoint : mountPoints) {
			IProject mp = mountPoint.getProject();
			if (mp.equals(unloadedProject)) {
				EObject mountedPoint = mountPoint.getMountedPoint();
				if (mountedPoint instanceof Element) {
					mountedSharePoints.add((Element) mountedPoint);
				}
			}
		}
		this.context = (mountedSharePoints.isEmpty()) ? null : mountedSharePoints.iterator().next();
		
		if (mountedSharePoints.size() > 1) {
			StringBuffer buff = new StringBuffer();
			buff.append(String.format("%s - %s (project='%s' {ID=%s} - found %d mounted share point elements",
					ProjectUsageIntegrityPlugin.getInstance().getPluginName(),
					this.getName(),
					unloadedProject.getName(), unloadedProject.getProjectID(), mountedSharePoints.size()));
			for (Element e : mountedSharePoints) {
				buff.append(String.format("\n - %s : %s {ID=%s}", e.getHumanName(), e.getHumanType(), e.getID()));
			}
			helper.logger.warn(buff.toString());
		}
	}

	public String getMessage() { return this.message; }
	public Element getContext() { return this.context; }
	
	@Override
	protected void repair() {
		com.nomagic.ci.persistence.ProjectDescriptor ciDescriptor = unloadedProject.getProjectDescriptor();
		org.eclipse.emf.common.util.URI eURI = ciDescriptor.getLocationUri();
		java.net.URI jURI = ProjectUtilities.getURI(eURI);
		com.nomagic.magicdraw.core.project.ProjectDescriptor coreDescriptor = ProjectDescriptorsFactory.createProjectDescriptor(jURI);
		
		boolean allowUI = true;
		ModulesService.findOrLoadModule(project, coreDescriptor, allowUI);
	}
}
