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