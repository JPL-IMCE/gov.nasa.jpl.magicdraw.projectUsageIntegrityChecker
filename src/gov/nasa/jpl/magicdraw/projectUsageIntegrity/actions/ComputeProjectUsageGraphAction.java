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

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.commands.ComputeProjectUsageGraphCommand;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.utils.MDLog;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ComputeProjectUsageGraphAction extends NMAction implements AnnotationAction {

	private static final long serialVersionUID = 2935805727158434622L;

	protected Project mProject;
	
	public ComputeProjectUsageGraphAction(@Nonnull Project project) {
		super("PROJECT_USAGE_INTEGRITY_CHECKER_COMPUTE_PROJECT_USAGE_GRAPH", "ProjectUsage Integrity Checker Compute ProjectUsage Graph", 0);
		this.mProject = project;
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		computeProjectUsageGraph(this.mProject);
	}

	@Override
	public void execute(Collection<Annotation> annotations) {
		if (annotations == null || annotations.isEmpty()) return;
		
		for (Annotation annotation : annotations) {
			final BaseElement baseElement = annotation.getTarget();
			if (baseElement instanceof Project) {
				computeProjectUsageGraph((Project) baseElement);
			}
		}
	}

	protected void computeProjectUsageGraph(Project p) {
		ComputeProjectUsageGraphCommand c = new ComputeProjectUsageGraphCommand(p, true);
		c.run();
	}

	@Override
	public boolean canExecute(Collection<Annotation> annotations) {
		return true;
	}

	protected static void logError(@Nonnull Exception exception) {
		MDLog.getPluginsLog().error(exception.getMessage(), exception);
	}
}
