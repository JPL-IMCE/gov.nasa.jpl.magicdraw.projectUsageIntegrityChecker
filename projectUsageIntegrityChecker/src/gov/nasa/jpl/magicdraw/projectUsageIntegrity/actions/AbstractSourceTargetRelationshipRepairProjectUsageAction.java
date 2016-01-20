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

import java.lang.String;
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