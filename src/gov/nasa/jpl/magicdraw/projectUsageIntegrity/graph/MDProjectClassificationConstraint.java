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

public class MDProjectClassificationConstraint extends MDAbstractProjectUsage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8695983574988595601L;

	public static enum UsageConstraintLevel { WARNING, ERROR, OK };
	public static enum ClassificationLevel { DEPRECATED, INCUBATOR, RECOMMENDED };
	
	private ClassificationLevel classificationLevel;
	private UsageConstraintLevel usageConstraintLevel;
	private String label;

	public MDProjectClassificationConstraint(UsageConstraintLevel usageConstraintLevel, ClassificationLevel classificationLevel, String containedPackageQName) {
		super();
		this.classificationLevel = classificationLevel;
		this.usageConstraintLevel = usageConstraintLevel;
		this.label = String.format("%s => %s: %s", classificationLevel, usageConstraintLevel, containedPackageQName);
		setIndex(containedPackageQName);
	}
	
	public ClassificationLevel getClassificationLevel() { return classificationLevel; }
	
	public UsageConstraintLevel getUsageConstraintLevel() { return usageConstraintLevel; }
	
	public String getLabel() {
		return label;
	}

	public String toString() { return getLabel(); }
	public String getSignature() { return getLabel(); }

	@Override
	public boolean isReshared() { return false; }
	
}