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

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.icons.OffIcon;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.icons.OnIcon;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import com.nomagic.actions.NMStateAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;

/**
 * @author Donatas Simkunas (NoMagic)
 * @author Nicolas F Rouquette (JPL)
 * 
 * Adapted from <install root>/openapi/examples/actiontypes/SimpleStateAction.java
 */
public class ToggleProjectUsageIntegrityCheckerAction extends NMStateAction {

	private static final long serialVersionUID = -6170433837284995161L;

	public static final Icon ENABLED_ICON = new OnIcon();
	public static final Icon DISABLED_ICON = new OffIcon();

	public static final String ENABLED_DESCRIPTION = "JPL SSCAE ProjectUsage Integrity Checker Enabled";
	public static final String DISABLED_DESCRIPTION = "JPL SSCAE ProjectUsage Integrity Checker Disabled";

	private boolean iAmSelected = true;

	public ToggleProjectUsageIntegrityCheckerAction() {
		super("TOGGLE_PROJECT_USAGE_INTEGRITY_CHECKER_ACTION", "", 0);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// changing state
		iAmSelected = !iAmSelected;
		// showing changes
		updateState();

		if (iAmSelected) {
			Project p = Application.getInstance().getProject();
			if (null != p) {
				ProjectUsageIntegrityHelper helper = ProjectUsageIntegrityPlugin.getInstance().getSSCAEProjectUsageIntegrityProfileForProject(p);
				if (null != helper) {
					helper.runSSCAEValidationAndShowResultsIfCheckerEnabled(true);
				}
			}
		}
	}

	/**
	 * Must be called in on the Swing event thread.
	 */
	@Override
	public void setState(boolean newState) {
		this.iAmSelected = newState;
		super.setState(newState);
	}

	/**
	 * Must be called in on the Swing event thread.
	 */
	@Override
	public void updateState() {
		setState( iAmSelected );

		setDescription((iAmSelected ? ENABLED_DESCRIPTION : DISABLED_DESCRIPTION));
		setSmallIcon((iAmSelected ? ENABLED_ICON : DISABLED_ICON));

		ProjectUsageIntegrityPlugin.getInstance().checkCurrentProjectStatusAction.updateState();
		ProjectUsageIntegrityPlugin.getInstance().showCurrentProjectUsageGraphAction.updateState();
		ProjectUsageIntegrityPlugin.getInstance().toggleTeamworkOrAllGraphAction.updateState();
		ProjectUsageIntegrityPlugin.getInstance().toggleGraphLabelAction.updateState();
		ProjectUsageIntegrityPlugin.getInstance().toggleThreadedAction.updateState();
	}
}