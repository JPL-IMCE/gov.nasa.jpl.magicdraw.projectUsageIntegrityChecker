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
import com.nomagic.ci.persistence.local.ProjectState;
import com.nomagic.ci.persistence.local.spi.localproject.LocalAttachedProject;
import com.nomagic.magicdraw.core.Project;

public class MDLocalAttachedProject extends MDLocalProject implements MDAttachedProject {

	private SSCAEProjectDigest digest;

	private ProjectState state;

	public void setState(ProjectState state) {
		this.state = state;
	}

	public ProjectState getState() {
		return state;
	}

	public MDLocalAttachedProject() {
		super();
	}

	public static void configure(MDLocalAttachedProject that, @Nonnull Project rootProject, LocalAttachedProject p, String index) throws RemoteException {
		MDLocalProject.configure(that, rootProject, p, index);
		that.setState(p.getProjectState());
		that.setRoot(false);
		that.refresh(p);
	}

	@Override
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);

		assert (p instanceof LocalAttachedProject);
		LocalAttachedProject lap = (LocalAttachedProject) p;
		this.setState(lap.getProjectState());
		
		this.setLabel(String.format("[%s] '%s {ID=%s, loc=%s, md5=%s}", 
				this.getIndex(), this.getName(), this.getProjectID(), this.getLocation(), this.getMD5checksum()));

		this.setMDInfo(String.format("{%s, %s, %s, %s, %s}", 
					asBooleanFlag("isNew", p.isNew()), 
					asBooleanFlag("isReadOnly", p.isReadOnly()),
					asBooleanFlag("isManuallyReachable", lap.isManuallyReachable()),
					asBooleanFlag("isAvailable", lap.isProjectAvailable()),
					asBooleanFlag("isLoaded", lap.isLoaded())));
	}

	public SSCAEProjectDigest getDigest() {
		return digest;
	}

	public void setDigest(SSCAEProjectDigest digest) {
		this.digest = digest;
	}

	@Override
	public void updateIndex(String index) {		
		setLabel("[L" + index + "]" + getLabel().substring(2 + getIndex().length()));
		setIndex("L" + index);
	}
}