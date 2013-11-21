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

import gov.nasa.jpl.logfire.RunnableSessionWrapper;
import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityHelper;

import java.awt.event.ActionEvent;

import javax.annotation.Nonnull;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.SessionManager;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public abstract class AbstractRepairProjectUsageAction extends AbstractSSCAEAnnotationAction {
	
	private static final long serialVersionUID = 3323270601008851056L;
	
	public AbstractRepairProjectUsageAction(
			@Nonnull String ID,
			@Nonnull String label,
			int priority,
			@Nonnull ProjectUsageIntegrityHelper helper) {
		super(ID, label, 0, helper);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		
		new RunnableSessionWrapper(this.getName()) {

			@Override
			public void run() {
				if (!hasCurrentProjectUsageGraphCompatibleSignature()) {
					log.warn("stale annotation");
					Application.getInstance().getGUILog().log("Stale annotation: " + AbstractRepairProjectUsageAction.this.getName());
					return;
				}
				
				final SessionManager sm = SessionManager.getInstance();
				sm.createSession(AbstractRepairProjectUsageAction.this.getDescription());
				try {
					repair();
					if (sm.isSessionCreated())
						sm.closeSession();
				} catch (Exception exc) {
					logError(exc);
					if (sm.isSessionCreated())
						sm.cancelSession();
				}

			}
		};
		
		/*
		 * If a single annotation is selected and an action invoked, the event will be non-null.
		 * If multiple annotations are selected, the event will be null.
		 */
		if (null == event)
			return;
		
		refreshSSCAEValidation();
	}

	protected abstract void repair();
	
	@Override
	public int hashCode(){
		int i = 1;
		i = i + 31 * this.graphSignature.hashCode();
		i = i + 31 * this.getID().hashCode();

		return i;
	}
}
