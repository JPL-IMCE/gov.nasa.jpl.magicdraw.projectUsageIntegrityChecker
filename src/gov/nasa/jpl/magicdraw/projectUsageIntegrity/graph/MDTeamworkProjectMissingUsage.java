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

import java.rmi.RemoteException;

public class MDTeamworkProjectMissingUsage extends MDAbstractProjectUsage {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1258080359390185805L;

	public MDTeamworkProjectMissingUsage() {
		super();
	}
	
	public boolean isSticky() {
		return false;
	}
	
	public String getVersion() {
		return "[MissingUsage]";
	}

	public String getLabel() {
		return "[MissingUsage]";
	}
	
	public String getSignature() {
		return "[MissingUsage]";
	}
	
	public static void configure(MDTeamworkProjectUsage that, MDAbstractProject source, MDAbstractProject target) throws RemoteException {
		MDAbstractProjectUsage.configure(that, source, target);
	}

	public boolean isReshared() { return false; }
	public String toString() { return this.getLabel(); }

	public boolean isValidUsage() {
		return false;
	}
}