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
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class SSCAEUnloadedModuleAnnotation extends SSCAEAnnotation {

	public SSCAEUnloadedModuleAnnotation(
			@Nonnull String graphSignature,
			@Nonnull EnumerationLiteral level, 
			@Nonnull String message, 
			@Nonnull BaseElement element,
			@Nonnull List<? extends NMAction> actions) {
		super(graphSignature, level, "SSCAE Unloaded Module", message, element, actions);
	}
}
