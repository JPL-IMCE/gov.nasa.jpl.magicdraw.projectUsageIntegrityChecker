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

import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.magicdraw.core.modules.ModulesService;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class RepairSystemOrStandardProfileFlagAction extends AbstractRepairProjectUsageAction {

	private static final long serialVersionUID = -6527969457906756269L;

	protected final IPrimaryProject sourceP;
	
	public RepairSystemOrStandardProfileFlagAction(
			@Nonnull ProjectUsageIntegrityHelper helper, 
			@Nonnull IPrimaryProject sourceP) {
		super("PROJECT_USAGE_INTEGRITY_CHECKER_REPAIR_SYSTEM_OR_STANDARD_PROFILE_FLAG_ACTION", 
				String.format("Set the 'System/Standard Profile' flag for this project '%s'", sourceP.getName()), 
				0, helper);
		this.sourceP = sourceP;
	}

	@Override
	protected void repair() {
		ModulesService.setStandardSystemProfile(sourceP, true);
	}

}
