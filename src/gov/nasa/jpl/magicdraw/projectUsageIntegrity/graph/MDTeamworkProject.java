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

import java.lang.String;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.eclipse.emf.common.util.URI;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.teamwork.application.storage.ITeamworkProject;

public abstract class MDTeamworkProject extends MDAbstractProject {

	private URI location;
	private List<String> tags;
	private String label;
	private boolean isRoot;
	private String version;
	private String anonymizedVersion;
	private String fullVersion;

	public MDTeamworkProject() {
		super();
	}
	
	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAnonymizedVersion() {
		return anonymizedVersion;
	}

	public void setAnonymizedVersion(String anonymizedVersion) {
		this.anonymizedVersion = anonymizedVersion;
	}

	public String getFullVersion() {
		return fullVersion;
	}

	public void setFullVersion(String fullVersion) {
		this.fullVersion = fullVersion;
	}

	public void setLocation(URI location) {
		this.location = location;
	}

	public static void configure(MDTeamworkProject that, @Nonnull Project rootProject, ITeamworkProject p, String index) throws RemoteException {
		MDAbstractProject.configure(that, p, "T" + index);

		that.refresh(p);
	}
	
	@Override
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);
		assert (p instanceof ITeamworkProject);
		ITeamworkProject tp = (ITeamworkProject)p;
		
		this.setLocation(p.getLocationURI());
		List<String> pTags = tp.getTags();
		if (null == pTags || pTags.isEmpty()) {
			this.setTags(Collections.<String>emptyList());
			this.setLabel(String.format("[%s] '%s {ID=%s}", this.getIndex(), this.getName(), this.getProjectID()));
		} else {
			this.setTags(pTags);
			Collections.sort(this.getTags(), String.CASE_INSENSITIVE_ORDER);
			this.setLabel(String.format("[%s] '%s {ID=%s}", this.getIndex(), this.getName(), this.getProjectID()));
		}

	}

	public URI getLocation() { return this.location; }

	public boolean isProjectMissing() { return false; }

	public boolean isRootProject() { return this.isRoot; }

	public boolean isLocalTemplate() { return false; }

	public String toString() { return this.label; }
}