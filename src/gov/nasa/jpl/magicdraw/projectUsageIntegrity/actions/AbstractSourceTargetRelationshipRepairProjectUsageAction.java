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

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public abstract class AbstractSourceTargetRelationshipRepairProjectUsageAction extends AbstractRepairProjectUsageAction {
	
	private static final long serialVersionUID = 3323270601008851056L;
	
	protected final Model m;
	protected final IPrimaryProject sourceP;
	protected final IAttachedProject targetP;

	public AbstractSourceTargetRelationshipRepairProjectUsageAction(
			@Nonnull String ID,
			@Nonnull String label,
			@Nonnull ProjectUsageIntegrityHelper helper, 
			@Nonnull Model m, 
			@Nonnull IPrimaryProject sourceP, 
			@Nonnull IAttachedProject targetP) {
		super(ID, label, 0, helper);
		
		this.m = m;
		this.sourceP = sourceP;
		this.targetP = targetP;
	}

	@Override
	public int hashCode(){
		int i = 1;
		i = i + 31 * this.graphSignature.hashCode();
		i = i + 31 * ((this.m == null) ? 0 : this.m.hashCode());
		i = i + 31 * ((this.sourceP == null) ? 0 : this.sourceP.hashCode());
		i = i + 31 * ((this.targetP == null) ? 0 : this.targetP.hashCode());
		i = i + 31 * this.getID().hashCode();

		return i;
	}
}
