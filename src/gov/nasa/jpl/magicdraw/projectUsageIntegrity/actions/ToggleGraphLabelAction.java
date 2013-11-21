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

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
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
public class ToggleGraphLabelAction extends NMStateAction {

	private static final long serialVersionUID = -6170433837284995161L;
	
	public static final String ENABLED_LABEL = "N";
	public static final String DISABLED_LABEL = "A";
	
	public static final String ENABLED_DESCRIPTION = "JPL SSCAE ProjectUsage Integrity Toggle Graph Label - currently with module name label";
	public static final String DISABLED_DESCRIPTION = "JPL SSCAE ProjectUsage Integrity Toggle Graph Label - currently with anonymous module labels";
	
	private boolean showNames = true;
	
	public ToggleGraphLabelAction() {
		super("TOGGLE_GRAPH_LABEL_ACTION", "", 0);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// changing state
        showNames = !showNames;
        // showing changes
        updateState();
        
        if (showNames) {
        		Project p = Application.getInstance().getProject();
        		if (null != p) {
        			ProjectUsageIntegrityHelper helper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(p);
        			if (null != helper) {
        				helper.runSSCAEValidationAndShowResultsIfCheckerEnabled(true);
        			}
        		}
        }
	}
	
	@Override
	public void setState(boolean newState) {
		this.showNames = newState;
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
		
		setState( showNames );
		
		setDescription((showNames ? ENABLED_DESCRIPTION : DISABLED_DESCRIPTION));
		setName((showNames ? ENABLED_LABEL : DISABLED_LABEL));
	}
}
