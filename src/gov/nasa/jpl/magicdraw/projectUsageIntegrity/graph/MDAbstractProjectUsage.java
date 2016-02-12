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

import org.jgrapht.graph.DefaultEdge;

import com.nomagic.ci.metamodel.project.ProjectUsage;
// No longer exists
// import com.nomagic.ci.metamodel.relocate.UnresolvedProjectUsage;

public abstract class MDAbstractProjectUsage extends DefaultEdge {

	private static final long serialVersionUID = -6766006976667889404L;
	
	private MDAbstractProject source;
	private MDAbstractProject target;
	private String usageConsistencyLabel;
	
	private String index;
	abstract public boolean isReshared();
	private boolean readOnly;
	private boolean resolved;
	private boolean isNew;
	
	// MD17.0.5
	private boolean isAutomatic = false;
	
	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	public boolean isAutomatic() {
		return isAutomatic;
	}

	public void setAutomatic(boolean isAutomatic) {
		this.isAutomatic = isAutomatic;
	}

	public boolean isLoadedAutomatically() {
		return isLoadedAutomatically;
	}

	public void setLoadedAutomatically(boolean isLoadedAutomatically) {
		this.isLoadedAutomatically = isLoadedAutomatically;
	}

	public boolean isResharedAutomatically() {
		return isResharedAutomatically;
	}

	public void setResharedAutomatically(boolean isResharedAutomatically) {
		this.isResharedAutomatically = isResharedAutomatically;
	}

	public boolean isWithPrivateDependencies() {
		return isWithPrivateDependencies;
	}

	public void setWithPrivateDependencies(boolean isWithPrivateDependencies) {
		this.isWithPrivateDependencies = isWithPrivateDependencies;
	}
	
	// MD17.0.5
	private boolean isLoadedAutomatically = false;
	
	// MD17.0.5
	private boolean isResharedAutomatically = false;
	
	// MD17.0.5
	private boolean isWithPrivateDependencies = false;
	
	private String mdFlags;
	
	public String getMDFlags() {
		return mdFlags;
	}
	
	public void setMDFlags(String mdFlags) {
		this.mdFlags = mdFlags ;
	}
	
	public MDAbstractProjectUsage() {
		super();
	}
	
	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public void setSource(MDAbstractProject source) {
		this.source = source;
	}

	public void setTarget(MDAbstractProject target) {
		this.target = target;
	}

	public String getUsageConsistencyLabel() {
		return usageConsistencyLabel;
	}

	public void setUsageConsistencyLabel(String usageConsistencyLabel) {
		this.usageConsistencyLabel = usageConsistencyLabel;
	}

	public static void configure(MDAbstractProjectUsage that, MDAbstractProject source, MDAbstractProject target) {
		that.setIndex(source.getIndex() + " => " + target.getIndex());
		that.setSource(source);
		that.setTarget(target);
		that.setReadOnly(true);
		that.setResolved(false || SSCAEProjectUsageGraph.RESOLVED_FLAG_OVERRIDE);
	}
	
	public static void configure(MDAbstractProjectUsage that, MDAbstractProject source, MDAbstractProject target, ProjectUsage pu) {
		if (pu == null) 
			throw new IllegalArgumentException("ProjectUsage should be non null!");
		
		that.setIndex(source.getIndex() + " => " + target.getIndex());
		that.setSource(source);
		that.setTarget(target);
		boolean isResolved = true;
		
		// no longer exists
		//if (pu instanceof UnresolvedProjectUsage)
		//	isResolved = false;

		if (!SSCAEProjectUsageGraph.getMDRelativeURI(pu.getUsedProjectURI()).equals(target.getLocation()))
			isResolved = false;

		that.setReadOnly(pu.isReadonly());
		that.setResolved(isResolved || SSCAEProjectUsageGraph.RESOLVED_FLAG_OVERRIDE);
		
		that.setAutomatic(pu.isAutomatic());
		that.setLoadedAutomatically(pu.isLoadedAutomatically());
		that.setNew(pu.isNew());
		that.setResharedAutomatically(pu.isResharedAutomatically());
		that.setWithPrivateDependencies(pu.isWithPrivateDependencies());
		
		that.setMDFlags(String.format("[isNew=%d, isAutomatic=%d, isLoadedAutomatically=%d, isReadonly=%d, isReshared=%d, isResharedAutomatically=%d, isWithPrivateDependencies=%d, version='%s']",
				getFlag(pu.isNew()),
				getFlag(pu.isAutomatic()),
				getFlag(pu.isLoadedAutomatically()),
				getFlag(pu.isReadonly()),
				getFlag(pu.isReshared()),
				getFlag(pu.isResharedAutomatically()),
				getFlag(pu.isWithPrivateDependencies()),
				(pu.getVersion() == null ? "<none>" : pu.getVersion())));
	}

	public static int getFlag(boolean b) {
		return (b ? 1 : 0);
	}
	
	protected String getResharedLabel() { return ((this.isReshared()) ? "shared" : "!shared"); }
	protected String getReadOnlyLabel() { return ((this.readOnly) ? "R/O" : "R/W"); }
	protected String getResolvedLabel() { return ((this.resolved) ? "" : ", !resolved"); }

	public MDAbstractProject getSource() { return this.source; }
	public MDAbstractProject getTarget() { return this.target; }
	public abstract String getSignature();

	public boolean isValidUsage() {
		return this.readOnly && this.isReshared() && this.resolved;
	}
}