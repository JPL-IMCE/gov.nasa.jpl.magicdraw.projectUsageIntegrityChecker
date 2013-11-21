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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation;

import java.util.List;

import javax.annotation.Nonnull;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class SSCAEAnnotation extends Annotation {
	
	public final String graphSignature;
	
	public SSCAEAnnotation(
			@Nonnull String graphSignature,
			@Nonnull EnumerationLiteral level, 
			@Nonnull String abbrev, 
			@Nonnull String message, 
			@Nonnull BaseElement element) {
		super(level, abbrev, message, element);
		this.graphSignature = graphSignature;
	}
	
	public SSCAEAnnotation(
			@Nonnull String graphSignature,
			@Nonnull EnumerationLiteral level, 
			@Nonnull String abbrev, 
			@Nonnull String message, 
			@Nonnull BaseElement element,
			@Nonnull List<? extends NMAction> actions) {
		super(level, abbrev, message, element, actions);
		this.graphSignature = graphSignature;
	}
	
	public SSCAEAnnotation(
			@Nonnull String graphSignature,
			@Nonnull BaseElement element, 
			@Nonnull Constraint constraint, 
			@Nonnull String message, 
			@Nonnull List<? extends NMAction> actions) {
		super(element, constraint, message, actions);
		this.graphSignature = graphSignature;
	}
}
