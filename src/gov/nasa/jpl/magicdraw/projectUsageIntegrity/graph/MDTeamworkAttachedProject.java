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
import com.nomagic.ci.persistence.local.IUpdateAdvisor;
import com.nomagic.ci.persistence.local.ProjectState;
import com.nomagic.ci.persistence.versioning.IVersionDescriptor;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkAttachedProject;

public class MDTeamworkAttachedProject extends MDTeamworkProject implements MDAttachedProject  {

	private SSCAEProjectDigest digest;
	
	private ProjectState state;
	
	public void setState(ProjectState state) {
		this.state = state;
	}
	
	public ProjectState getState() {
		return state;
	}
	
	public MDTeamworkAttachedProject() {
		super();
	}
	
	public static void configure(MDTeamworkAttachedProject that, @Nonnull Project rootProject, TeamworkAttachedProject p, String index) throws RemoteException {
		MDTeamworkProject.configure(that, rootProject, p, index);
		that.setRoot(false);
		that.refresh(p);
	}

	@Override
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);
		
		assert (p instanceof TeamworkAttachedProject);
		TeamworkAttachedProject tap = (TeamworkAttachedProject) p;
		this.setState(tap.getProjectState());
		
		final @Nonnull IVersionDescriptor version = ProjectUtilities.getVersion(tap);
		this.setVersion(version.getName());

		final String teamworkRemoteId = ProjectUtilities.getTeamworkRemoteId(p);
		final @Nonnull ProjectDescriptor descriptor = TeamworkUtils.getRemoteProjectDescriptor(teamworkRemoteId);
		
		if (null == descriptor) {
			this.setFullVersion("[server not available]#" + this.getVersion());
		} else {
			this.setFullVersion(ProjectDescriptorsFactory.getProjectFullPath(descriptor.getURI()) + "#" + this.getVersion());
		}
		this.setAnonymizedVersion(String.format("%s#%s", this.getIndex(), this.getVersion()));
		
		this.setMDInfo(String.format("{%s, %s, %s, %s, %s, %s, %s, %s, %s, %s}", 
					asBooleanFlag("isNew", p.isNew()), 
					asBooleanFlag("isReadOnly", p.isReadOnly()),
					asBooleanFlag("isHistorical", tap.isHistorical()),
					asBooleanFlag("isManuallyReachable", tap.isManuallyReachable()),
					asBooleanFlag("isAvailable", tap.isProjectAvailable()),
					asBooleanFlag("isLoaded", tap.isLoaded()),
					asBooleanFlag("isReadFromTeamwork", tap.isReadFromTeamwork()),
					asBooleanFlag("hasUpdateAdvisor", tap.getService(IUpdateAdvisor.class) != null),
					asBooleanFlag("isUpToDate", tap.isUpToDate()),
					asBooleanFlag("isLocalUpdate", tap.isLocalUpdate())));
	}
	
	public SSCAEProjectDigest getDigest() {
		return digest;
	}

	public void setDigest(SSCAEProjectDigest digest) {
		this.digest = digest;
	}
	
	@Override
	public void updateIndex(String index) {
		setLabel("[T" + index + "]" + getLabel().substring(2 + getIndex().length()));
		setIndex("T" + index);
	}
	
	@Override
	public String getMDInfo() {
		// TODO Auto-generated method stub
		return null;
	}
}