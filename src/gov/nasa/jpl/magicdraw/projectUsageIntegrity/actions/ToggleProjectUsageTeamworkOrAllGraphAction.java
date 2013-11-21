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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;

import java.awt.event.ActionEvent;

import com.nomagic.actions.NMStateAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;

/**
 * @author Donatas Simkunas (NoMagic)
 * @author Nicolas F Rouquette (JPL)
 * 
 * Adapted from <install root>/openapi/examples/actiontypes/SimpleStateAction.java
 */
public class ToggleProjectUsageTeamworkOrAllGraphAction extends NMStateAction {
	
	private static final long serialVersionUID = 4923058989527721445L;
	public static final String ENABLED_DESCRIPTION = "Show Teamwork-only ProjectUsage Graph";
	public static final String DISABLED_DESCRIPTION = "Show Local and Teamwork ProjectUsage Graph";
	
	public static final String ENABLED_LABEL = "Teamwork";
	public static final String DISABLED_LABEL = "Show All";
	
	private boolean iAmSelected = true;
	
	public ToggleProjectUsageTeamworkOrAllGraphAction() {
		super("TOGGLE_PROJECT_USAGE_TEAMWORK_OR_ALL_GRAPH_ACTION", ENABLED_LABEL, 0);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// changing state
        iAmSelected = !iAmSelected;
        // showing changes
        updateState();
	}

	@Override
	public void setState(boolean newState) {
		this.iAmSelected = newState;
		super.setState(newState);
	}
	
	@Override
	public void updateState() {
		if (!ProjectUsageIntegrityPlugin.getInstance().isProjectUsageIntegrityCheckerEnabled()) {
			setEnabled(false);
			return;
		} else {
			setEnabled(true);
		}
		
		Project project = Application.getInstance().getProject();
		setEnabled(project != null);
		
		setState( iAmSelected );
		setDescription((iAmSelected ? ENABLED_DESCRIPTION : DISABLED_DESCRIPTION));
		setName((iAmSelected ? ENABLED_LABEL : DISABLED_LABEL));
	}
}
