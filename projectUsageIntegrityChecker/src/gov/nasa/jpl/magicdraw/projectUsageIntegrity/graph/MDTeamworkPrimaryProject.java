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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.lang.String;
import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.teamwork.application.storage.ITeamworkProject;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkPrimaryProject;

public class MDTeamworkPrimaryProject extends MDTeamworkProject {

	public MDTeamworkPrimaryProject() {
		super();
	}
	
	public static void configure(MDTeamworkProject that, @Nonnull Project rootProject, ITeamworkProject p, String index) throws RemoteException {
		MDTeamworkProject.configure(that, rootProject, p, index);
		that.setRoot(true);
		that.refresh(p);
		that.setVersion("<not computed for a teamwork primary project1>");
		that.setFullVersion("<not computed for a teamwork primary project2>");
		that.setAnonymizedVersion("<not computed for a teamwork primary project3>");
	}
	

	@Override
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);
		
		assert (p instanceof TeamworkPrimaryProject);
		TeamworkPrimaryProject tpp = (TeamworkPrimaryProject) p;
		
		this.setMDInfo(String.format("{%s, %s, %s, %s, %s, %s}", 
					asBooleanFlag("isNew", p.isNew()), 
					asBooleanFlag("isReadOnly", p.isReadOnly()),
					asBooleanFlag("isHistorical", tpp.isHistorical()),
					asBooleanFlag("isAvailable", tpp.isProjectAvailable()),
					asBooleanFlag("isReadFromTeamwork", tpp.isReadFromTeamwork()),
					asBooleanFlag("isUpToDate", tpp.isUpToDate())));
	}
}