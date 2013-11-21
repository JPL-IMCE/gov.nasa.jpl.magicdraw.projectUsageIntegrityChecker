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
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.MDAbstractProject;

import java.util.Collections;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.magicdraw.core.modules.ModuleUsage;
import com.nomagic.magicdraw.core.modules.ModulesService;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class RepairProjectUsageToReadOnlyAction extends AbstractSourceTargetRelationshipRepairProjectUsageAction {

	private static final long serialVersionUID = 6762271124846730853L;

	public RepairProjectUsageToReadOnlyAction(@Nonnull ProjectUsageIntegrityHelper helper, 
			@Nonnull Model m, 
			@Nonnull IPrimaryProject sourceP, 
			@Nonnull MDAbstractProject u,
			@Nonnull MDAbstractProject v,
			@Nonnull IAttachedProject targetP) {
		super("PROJECT_USAGE_INTEGRITY_CHECKER_REPAIR_PROJECT_USAGE_TO_READ_ONLY_ACTION", 
				String.format("Set the ProjectUsage relationship from '%s' to '%s' as {read-only}", u.getName(), v.getName()), 
				helper, m, sourceP, targetP);
	}

	@Override
	protected void repair() {
		final ModuleUsage mu = new ModuleUsage(sourceP, targetP);
		ModulesService.setReadOnlyOnTask(Collections.singleton(mu), true);
	}

}
