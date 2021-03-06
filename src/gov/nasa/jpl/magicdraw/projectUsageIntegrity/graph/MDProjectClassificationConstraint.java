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