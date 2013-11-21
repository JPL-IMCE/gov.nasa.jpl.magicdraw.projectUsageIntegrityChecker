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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.actions;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.SSCAEProjectUsageGraph;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation.SSCAEAnnotation;

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
