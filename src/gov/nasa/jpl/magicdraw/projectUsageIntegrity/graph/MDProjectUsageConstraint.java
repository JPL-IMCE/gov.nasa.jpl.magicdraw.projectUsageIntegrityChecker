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

public class MDProjectUsageConstraint extends MDAbstractProjectUsage {

	private static final long serialVersionUID = 2030011297308540642L;

	public static enum UsageConstraintLevel { WARNING, ERROR, OK };
	
	private UsageConstraintLevel usageConstraintLevel;
	private String label;

	public MDProjectUsageConstraint(UsageConstraintLevel usageConstraintLevel, String containedPackageQName) {
		super();
		this.usageConstraintLevel = usageConstraintLevel;
		this.label = String.format("%s: %s", usageConstraintLevel, containedPackageQName);
	}
	
	public UsageConstraintLevel getUsageConstraintLevel() { return usageConstraintLevel; }
	
	public String getLabel() {
		return label;
	}

	public String toString() { return getLabel(); }
	public String getSignature() { return getLabel(); }

	@Override
	public boolean isReshared() { return false; }
	
}