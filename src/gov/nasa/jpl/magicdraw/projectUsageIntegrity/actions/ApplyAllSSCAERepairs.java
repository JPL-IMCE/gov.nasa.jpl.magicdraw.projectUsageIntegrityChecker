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

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.core.Project;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class ApplyAllSSCAERepairs extends AbstractSSCAEAnnotationAction {
	
	private static final long serialVersionUID = -3832928815527962045L;
	
	protected final Project project;
	protected final ProjectUsageIntegrityHelper helper;
	
	public ApplyAllSSCAERepairs(
			@Nonnull Project project,
			@Nonnull ProjectUsageIntegrityHelper helper) {
		super("SSCAE_APPLY_ALL_SSCAE_REPAIRS", "Apply Selected SSCAE Repairs", 0, helper);
		this.project = project;
		this.helper = helper;
	}

	@Override
	public boolean canExecute(Collection<Annotation> annotations) {
		if (! super.canExecute(annotations))
			return false;
		
		if (annotations.isEmpty())
			return false;
		
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		// do nothing; all the work is done by execute()
	}
	
	@Override
	public int hashCode(){
		int i = 1;
		i = i + 31 * this.graphSignature.hashCode();
		i = i + 31 * this.getID().hashCode();

		return i;
	}
}
