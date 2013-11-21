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

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.ci.persistence.decomposition.ProjectAttachmentConfiguration;
import com.nomagic.magicdraw.core.modules.ModulesService;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class RepairAddMissingUsageRelationshipAction extends AbstractSourceTargetRelationshipRepairProjectUsageAction {

	private static final long serialVersionUID = 2839853632692095413L;

	protected final ProjectAttachmentConfiguration config;
	
	public RepairAddMissingUsageRelationshipAction(
			@Nonnull ProjectUsageIntegrityHelper helper, 
			@Nonnull Model m, 
			@Nonnull IPrimaryProject sourceP, 
			@Nonnull IAttachedProject targetP, 
			@Nonnull ProjectAttachmentConfiguration config) {
		super("PROJECT_USAGE_INTEGRITY_CHECKER_REPAIR_ADD_MISSING_USAGE_RELATIONSHIP_ACTION", 
				String.format("Add a direct ProjectUsage relationship from '%s' to '%s'", m.getName(), targetP.getName()), 
				helper, m, sourceP, targetP);
		this.config = config;
	}

	@Override
	protected void repair() {
		ProjectAttachmentConfiguration directAttachmentConfig = new ProjectAttachmentConfiguration(targetP.getLocationURI(), config);
		ModulesService.attachModuleOnTask(sourceP, directAttachmentConfig); 	// SSCAES-794: observable by notification via com.nomagic.ci.persistence.ProjectListener (POST_ATTACH_PROJECT)
		ModulesService.setReSharedOnTask(sourceP, targetP, true);				// SSCAES-794: non-observable by notification via com.nomagic.ci.persistence.ProjectListener
	}

}
