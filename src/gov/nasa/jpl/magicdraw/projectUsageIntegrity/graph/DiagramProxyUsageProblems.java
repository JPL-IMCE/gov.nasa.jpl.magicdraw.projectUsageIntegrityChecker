package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.util.Map;
import java.util.Set;

import com.nomagic.magicdraw.uml.DiagramType;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

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

public class DiagramProxyUsageProblems {

	private String qualifiedName;
	private String type;
	private String ID;
	private int proxyCount;
	
	public DiagramProxyUsageProblems() {
	}

	public DiagramProxyUsageProblems(final Map.Entry<DiagramPresentationElement, Set<Element>> entry) {
		DiagramPresentationElement dpe = entry.getKey();
		Diagram d = dpe.getDiagram();
		DiagramType dType = dpe.getDiagramType();
		Set<Element> dpeProxies = entry.getValue();
		this.setQualifiedName(d.getQualifiedName());
		this.setID(d.getID());
		this.setType(dType.getType());
		this.setProxyCount(dpeProxies.size());
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public void setQualifiedName(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public int getProxyCount() {
		return proxyCount;
	}

	public void setProxyCount(int proxyCount) {
		this.proxyCount = proxyCount;
	}
	
}
