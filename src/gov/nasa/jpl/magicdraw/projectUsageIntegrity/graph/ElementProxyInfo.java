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

import java.util.Collections;
import java.util.List;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.expression.ElementPath;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class ElementProxyInfo {

	private List<ElementPathSegment> elementPath;
	
	public ElementProxyInfo() {
	}

	public ElementProxyInfo(final Element owner, final Element proxy) {
		this.setElementPath(ElementPath.getElementPathFrom(owner, proxy));
	}
	
	public ElementProxyInfo(final Element proxy) {
		this.setElementPath(Collections.singletonList(ElementPath.getElementNameSegment(proxy)));
	}
	
	public List<ElementPathSegment> getElementPath() {
		return elementPath;
	}

	public void setElementPath(List<ElementPathSegment> elementPath) {
		this.elementPath = elementPath;
	}
}
