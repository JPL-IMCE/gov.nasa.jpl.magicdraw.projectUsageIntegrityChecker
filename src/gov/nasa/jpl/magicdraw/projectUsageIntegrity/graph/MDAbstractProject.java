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

import org.eclipse.emf.common.util.URI;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.local.ProjectState;

public abstract class MDAbstractProject implements Comparable<MDAbstractProject> {

	private String index;
	private String projectID;
	private boolean isReadOnly;
	private String name;
	private ProjectClassification classification;
	
	// MD17.0.5
	private boolean isNew;
	
	public MDAbstractProject() {}
	
	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getProjectID() {
		return projectID;
	}

	public void setProjectID(String projectID) {
		this.projectID = projectID;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	public void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ProjectClassification getClassification() {
		return classification;
	}

	public void setClassification(ProjectClassification classification) {
		this.classification = classification;
	}
	
	public boolean isProject() {
		return this.classification == ProjectClassification.IS_PROJECT ||
				this.classification == ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS ||
				this.classification == ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_MISSING_ELEMENTS ||
				this.classification == ProjectClassification.IS_PROJECT_WITH_PROXIES_FOR_RECOVERED_ELEMENTS;
	}
	
	public boolean isModule() {
		return this.classification == ProjectClassification.IS_MODULE ||
				this.classification == ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS ||
				this.classification == ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS ||
				this.classification == ProjectClassification.IS_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS;
	}
	
	public boolean isHybrid() {
		return this.classification == ProjectClassification.IS_HYBRID_PROJECT_MODULE ||
				this.classification == ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS ||
				this.classification == ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS ||
				this.classification == ProjectClassification.IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS;
	}
	
	public boolean isUnloaded() {
		return this.classification == ProjectClassification.IS_UNLOADED;
	}
	

	public boolean isNotLoaded() {
		return this.classification == ProjectClassification.IS_NOT_LOADED;
	}
	
	@Override
	public int compareTo(MDAbstractProject that) {
		return this.getIndex().compareTo(that.getIndex());
	}
	
	public static void configure(MDAbstractProject that, IProject p, String index) {
		that.setIndex(index);
		that.setProjectID(p.getProjectID());
		that.setNew(p.isNew());
	}

	public void refresh(IProject p) throws RemoteException {
		this.setReadOnly(p.isReadOnly());
		this.setName(p.getName());
		this.setNew(p.isNew());
	}
	
	public abstract URI getLocation();
	public abstract boolean isProjectMissing();
	public abstract boolean isRootProject();
	public abstract boolean isLocalTemplate();
	
	private String mdInfo;
	public void setMDInfo(String mdInfo) { this.mdInfo = mdInfo; }
	public String getMDInfo() { return mdInfo; }
	
	public static String asBooleanFlag(String flagName, boolean flag) {
		return (flag ? flagName : "!" + flagName);
	}
}