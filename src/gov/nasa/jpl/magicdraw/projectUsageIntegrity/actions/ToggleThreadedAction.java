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

/**
 * @author Donatas Simkunas (NoMagic)
 * @author Alek Kerzhner (JPL)
 * 
 * Adapted from <install root>/openapi/examples/actiontypes/SimpleStateAction.java
 */
public class ToggleThreadedAction extends NMStateAction {

	private static final long serialVersionUID = -6170433837284995161L;
	
	public static final String ENABLED_LABEL = "BB";
	public static final String DISABLED_LABEL = "CS";
	
	public static final String ENABLED_DESCRIPTION = "WARNING - Running in threaded mode";
	public static final String DISABLED_DESCRIPTION = "Threading mode disabled";
	
	private boolean threadedMode = false;
	
	public ToggleThreadedAction() {
		super("TOGGLE_THREADED_ACTION", "", 0);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// changing state
		threadedMode = !threadedMode;
        // showing changes
        updateState();
	}
	
	/**
	 * Must be called in on the Swing event thread.
	 */	
	@Override
	public void setState(boolean newState) {
		this.threadedMode = newState;
		super.setState(newState);
	}

	/**
	 * Must be called in on the Swing event thread.
	 */
	@Override
	public void updateState() {
		if (!ProjectUsageIntegrityPlugin.getInstance().isProjectUsageIntegrityCheckerEnabled()) {
			setEnabled(false);
			return;
		} else {
			setEnabled(true);
		}
		
		setState( threadedMode );
		
		setDescription((threadedMode ? ENABLED_DESCRIPTION : DISABLED_DESCRIPTION));
		setName((threadedMode ? ENABLED_LABEL : DISABLED_LABEL));
	}
}
