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

import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.jmi.EventSupport;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
@SuppressWarnings("unused")
public class UnapplyStereotypeAction extends AbstractRepairProjectUsageAction {

	protected final Element mElement;
	protected final Stereotype mStereotype;

	public UnapplyStereotypeAction(
			@Nonnull Element element, 
			@Nonnull Stereotype stereotype,
			@Nonnull ProjectUsageIntegrityHelper helper) {
		super("PROJECT_USAGE_INTEGRITY_CHECKER_UNAPPLY_STEREOTYPE_ACTION", 
				String.format("Unapply <<%s>> to '%s'", stereotype.getName(), element.getHumanName()), 
				0, helper);
		this.mElement = element;
		this.mStereotype = stereotype;
	}

	private static final long serialVersionUID = -2268762778199313916L;

	@Override
	protected void repair() {
		if (StereotypesHelper.hasStereotypeOrDerived(mElement, mStereotype)) {
			StereotypesHelper.removeStereotype(mElement, mStereotype);
		}
	}

}
