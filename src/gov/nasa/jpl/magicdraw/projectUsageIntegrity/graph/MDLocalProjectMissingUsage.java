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


public class MDLocalProjectMissingUsage extends MDAbstractProjectUsage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7520079308670427612L;
	
	private String label;
	private String signature;

	public MDLocalProjectMissingUsage() {
		super();
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public boolean isReshared() {
		return true;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public static void configure(MDLocalProjectMissingUsage that, MDAbstractProject source, MDAbstractProject target) {
		MDAbstractProjectUsage.configure(that, source, target);
		that.setSignature(String.format("{%s, %s}", that.getResharedLabel(), that.getReadOnlyLabel()));
		that.setLabel(String.format("[%s] {%s, %s%s}", that.getIndex(), that.getResharedLabel(), that.getReadOnlyLabel(), that.getResolvedLabel()));
	}

	public String toString() { return this.label; }
	public String getSignature() { return this.signature; }
	
}