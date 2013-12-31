package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.util.List;

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

public class RecoveredElementProxy {

	private String elementPath;
	private List<ElementProxyInfo> proxies;
	
	public RecoveredElementProxy() {
	}

	public String getElementPath() {
		return elementPath;
	}

	public void setElementPath(String elementPath) {
		this.elementPath = elementPath;
	}

	public int getOwnedProxyCount() {
		return (proxies == null) ? 0 : proxies.size();
	}
	
	public List<ElementProxyInfo> getOwnedElementProxies() {
		return proxies;
	}

	public void setOwnedElementProxies(List<ElementProxyInfo> proxies) {
		this.proxies = proxies;
	}
}
