/*
 *
 * License Terms
 *
 * Copyright (c) 2013-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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