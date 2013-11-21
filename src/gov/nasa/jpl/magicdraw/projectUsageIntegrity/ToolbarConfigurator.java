/**
 * Copyright (c) 2002 NoMagic, Inc. All Rights Reserved.
 * 
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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity;

import javax.annotation.Nonnull;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.NMAction;

public class ToolbarConfigurator implements AMConfigurator {

	public static String PROJECT_USAGE_INTEGRITY_CATEGORY_ID = "PROJECT_USAGE_INTEGRITY_ACTIONS";
	public static String PROJECT_USAGE_INTEGRITY_CATEGORY_NAME = "ProjectUsage Integrity Actions";
	
	private NMAction action;
	
	public ToolbarConfigurator(@Nonnull NMAction action) {
		this.action = action;
	}
	
	@Override
	public int getPriority() {
		return AMConfigurator.MEDIUM_PRIORITY;
	}

	@Override
	public void configure(ActionsManager manager) {
		ActionsCategory category = (ActionsCategory) manager.getActionFor(PROJECT_USAGE_INTEGRITY_CATEGORY_ID);
		if (null == category) {
			category = new ActionsCategory(PROJECT_USAGE_INTEGRITY_CATEGORY_ID, PROJECT_USAGE_INTEGRITY_CATEGORY_NAME);
			category.setNested(true);
			manager.addCategory(category);
		}
		
		category.addAction(this.action);
	}

}
