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

import com.nomagic.ci.metamodel.project.ProjectUsage;

public class MDTeamworkProjectUsage extends MDAbstractProjectUsage {

	private static final long serialVersionUID = 1215405586462974764L;
	
	private boolean sticky;
	private String version;
	private String label;
	private String signature;
	private boolean reShared;
	
	public MDTeamworkProjectUsage() {
		super();
	}
	
	
	public boolean isSticky() {
		return sticky;
	}


	public void setSticky(boolean sticky) {
		this.sticky = sticky;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public String getLabel() {
		return label;
	}


	public void setLabel(String label) {
		this.label = label;
	}


	public boolean isReShared() {
		return reShared;
	}


	public void setReShared(boolean reShared) {
		this.reShared = reShared;
	}


	public void setSignature(String signature) {
		this.signature = signature;
	}

	public static void configure(MDTeamworkProjectUsage that, MDAbstractProject source, MDAbstractProject target, ProjectUsage pu) throws RemoteException {
		MDAbstractProjectUsage.configure(that, source, target, pu);
		that.setSticky(pu.isSticky());
		that.setVersion(pu.getVersion());
		that.setReShared(pu.isReshared());
		if (that.isSticky()) {
			that.setSignature(String.format(
					"{version=%s [Sticky], %s, %s}", that.getVersion(),
					that.getResharedLabel(), that.getReadOnlyLabel()));
			that.setLabel(String.format(
					"{version=%s [Sticky], %s, %s%s}", that.getVersion(),
					that.getResharedLabel(), that.getReadOnlyLabel(), that.getResolvedLabel()));
		} else {
			that.setSignature(String.format(
					"{version=[Latest], %s, %s}",
					that.getResharedLabel(), that.getReadOnlyLabel()));
			that.setLabel(String.format(
					"{version=[Latest], %s, %s%s}",
					that.getResharedLabel(), that.getReadOnlyLabel(), that.getResolvedLabel()));
		}
	}

	public boolean isReshared() { return this.reShared; }
	public String toString() { return this.label; }
	public String getSignature() { return this.signature; }

	public boolean isValidUsage() {
		return super.isValidUsage() && this.sticky;
	}
}