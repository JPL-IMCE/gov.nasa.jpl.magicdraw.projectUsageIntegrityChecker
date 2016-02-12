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
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEAnnotation;

import java.lang.String;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public abstract class AbstractSSCAEAnnotationAction extends NMAction implements AnnotationAction {

	private static final long serialVersionUID = 7435000522099905854L;

	protected final ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();
		
	protected final String graphSignature;
	protected final ProjectUsageIntegrityHelper helper;
	
	public AbstractSSCAEAnnotationAction(
			@Nonnull String ID, 
			@Nonnull String label, 
			int priority,
			@Nonnull ProjectUsageIntegrityHelper helper) {
		super(ID, label, priority);
		this.setDescription("PUIC Repair: " + label);
		this.helper = helper;
		SSCAEProjectUsageGraph g = helper.latestProjectUsageGraph;
		this.graphSignature = (null == g) ? "" : g.getProjectUsageGraphSignature();
	}

	protected void refreshSSCAEValidation() {
		helper.runSSCAEValidationAndShowResultsIfCheckerEnabled(false);		
	}
	
	// @see https://support.nomagic.com/browse/MDUMLCS-8942
	@Override
	public void execute(Collection<Annotation> annotations) {
		if (annotations == null || annotations.isEmpty()) return;

		for (Annotation annotation : annotations) {
			for (NMAction action : annotation.getActions()) {
				action.actionPerformed(null);
			}
		}
		
		refreshSSCAEValidation();
	}
	
	@Override
	public boolean canExecute(Collection<Annotation> annotations) {
		if (!hasCurrentProjectUsageGraphCompatibleSignature())
			return false;
		
		for (Annotation a : annotations) {
			if (!(a instanceof SSCAEAnnotation))
				return false;
			SSCAEAnnotation sa = (SSCAEAnnotation) a;
			if (!(this.graphSignature.equals(sa.graphSignature)))
				return false;
		}
		return true;
	}

	protected boolean hasCurrentProjectUsageGraphCompatibleSignature() {
		Project p = Application.getInstance().getProject();
		if (null == p)
			return false;
		
		ProjectUsageIntegrityHelper pHelper = plugin.getSSCAEProjectUsageIntegrityProfileForProject(p);
		if (null == pHelper)
			return false;
		
		SSCAEProjectUsageGraph pGraph = pHelper.latestProjectUsageGraph;
		if (null == pGraph)
			return false;
		
		if (this.graphSignature.length() == 0)
			return false;
		
		return (this.graphSignature.equals(pGraph.getProjectUsageGraphSignature()));
	}
	
	protected static void logError(@Nonnull Exception exception) {
		MDLog.getPluginsLog().error(exception.getMessage(), exception);
	}
}