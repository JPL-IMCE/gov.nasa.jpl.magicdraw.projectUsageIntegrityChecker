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

import org.jgrapht.graph.DefaultEdge;

import com.nomagic.ci.metamodel.project.ProjectUsage;
import com.nomagic.ci.metamodel.relocate.UnresolvedProjectUsage;

public abstract class MDAbstractProjectUsage extends DefaultEdge {

	private static final long serialVersionUID = -6766006976667889404L;
	
	private MDAbstractProject source;
	private MDAbstractProject target;
	private String usageConsistencyLabel;
	
	private String index;
	abstract public boolean isReshared();
	private boolean readOnly;
	private boolean resolved;

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
		if (pu instanceof UnresolvedProjectUsage)
			isResolved = false;

		if (!SSCAEProjectUsageGraph.getMDRelativeURI(pu.getUsedProjectURI()).equals(target.getLocation()))
			isResolved = false;

		that.setReadOnly(pu.isReadonly());
		that.setResolved(isResolved || SSCAEProjectUsageGraph.RESOLVED_FLAG_OVERRIDE);
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