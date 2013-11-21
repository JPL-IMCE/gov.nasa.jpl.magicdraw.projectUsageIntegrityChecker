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

import com.nomagic.ci.metamodel.project.ProjectUsage;

public class MDLocalProjectUsage extends MDAbstractProjectUsage {

	private static final long serialVersionUID = 8944818415106149812L;

	private String label;
	private String signature;
	private boolean reShared;

	public MDLocalProjectUsage() {
		super();
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

	public static void configure(MDLocalProjectUsage that, MDAbstractProject source, MDAbstractProject target, ProjectUsage pu) {
		MDAbstractProjectUsage.configure(that, source, target, pu);
		that.setReShared(pu.isReshared());
		that.setSignature(String.format("{%s, %s}", that.getResharedLabel(), that.getReadOnlyLabel()));
		that.setLabel(String.format("[%s] {%s, %s%s}", that.getIndex(), that.getResharedLabel(), that.getReadOnlyLabel(), that.getResolvedLabel()));
	}

	public String toString() { return this.label; }
	public String getSignature() { return this.signature; }
	
	/**
	 * MD 17.0.2 SP2: ProjectUsage.isReshared() == true for a local usage relationship.
	 * MD 17.0.2 SP3: ProjectUsage.isReshared() == false for a local usage relationship.
	 */
	public boolean isReshared() { return this.reShared; }
	
}