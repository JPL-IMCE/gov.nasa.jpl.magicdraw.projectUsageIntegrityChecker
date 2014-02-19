package gov.nasa.jpl.magicdraw.projectUsageIntegrity;
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

import org.eclipse.emf.common.util.URI;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.ProjectRepositoryEvent;
import com.nomagic.ci.persistence.local.ProjectRepositoryEventType;
import com.nomagic.ci.persistence.ProjectRepositoryListener;
import com.nomagic.ci.persistence.local.LocalProjectRepositoryEvent;

public class SSCAEProjectRepositoryListener implements ProjectRepositoryListener {

	protected ProjectUsageIntegrityHelper helper;
	
	public SSCAEProjectRepositoryListener(ProjectUsageIntegrityHelper projectUsageIntegrityHelper) {
		this.helper = projectUsageIntegrityHelper;
	}

	@Override
	public void notify(ProjectRepositoryEvent ev) {
		if (!helper.isEnabled())
			return;
		
		ProjectRepositoryEventType evType = ((LocalProjectRepositoryEvent) ev).getProjectRepositoryEventType();
	
		if (evType.isPostEvent()) {
			helper.hasPostEventNotifications = true;
		}
		
		URI uri =  ((LocalProjectRepositoryEvent) ev).getProjectURI();
		IProject p =  ((LocalProjectRepositoryEvent) ev).getProject();
		
		helper.logger.info(String.format("ProjectRepositoryEvent.notify(%s) project URI=%s has project? %s", evType.name(), uri, (p != null)));
	}

	public void dispose() {
		helper = null;
	}

}
