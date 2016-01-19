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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.transactions;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;

import java.beans.PropertyChangeEvent;
import java.lang.Object;
import java.lang.Runnable;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.ProjectEvent;
import com.nomagic.ci.persistence.ProjectListener;
import com.nomagic.ci.persistence.local.decomposition.DecompositionEvent;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.proxy.ProxyManager;
import com.nomagic.magicdraw.teamwork2.locks.ILockProjectService;
import com.nomagic.magicdraw.teamwork2.locks.LockInfo;
import com.nomagic.magicdraw.teamwork2.locks.LockService;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.transaction.ModelValidationResult;
import com.nomagic.uml2.transaction.TransactionCommitListener;

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 * @see https://support.nomagic.com/browse/MDUMLCS-13731
 *
 * Verify that every MD TransactionCommit involves changes that satisfy the following rules:
 * 1) deletion or modification of unlocked elements should be a consequence of updating the in memory model based on changes from the teamwork server
 * 2) outside of the context of a teamwork update or version change, MD should veto any transaction that results in modifying or deleting an unlocked element
 *
 */
@SuppressWarnings("deprecation")	
public class CollaborationIntegrityInvariantTransactionMonitor implements TransactionCommitListener, ProjectListener {

	static class UnlockedModificationInfo {
		public final ILockProjectService s;
		public final Element element;
		public final Element lockable;
		public final ModelValidationResult result;
		public final boolean canBeLocked;
		public final boolean isDeletion;
		public UnlockedModificationInfo(ILockProjectService s, Element element, Element lockable, ModelValidationResult result, boolean canBeLocked, boolean isDeletion) {
			this.s = s;
			this.element = element;
			this.lockable = lockable;
			this.result = result;
			this.canBeLocked = canBeLocked;
			this.isDeletion = isDeletion;
		}	
	}

	private volatile boolean illegalTransactionsDetected = false;

	public boolean illegalTransactionsDetected() { return illegalTransactionsDetected; }

	protected boolean between_pre_post_update = false;

	@Override
	public void notify(ProjectEvent event) {
		Logger log = MDLog.getPluginsLog();
		switch (event.getEventType()) {
		case PRE_UPDATE: between_pre_post_update = true; break;
		case POST_UPDATE: between_pre_post_update = false; break;
		case POST_ATTACH_PROJECT:
			if (event instanceof DecompositionEvent) {
				DecompositionEvent de = (DecompositionEvent) event;
				IProject attached = de.getAttachedProject();
				attached.getProjectListeners().add( this );
				if ( ApplicationEnvironment.isDeveloper() )
					log.info( String.format("### POST_ATTACH_PROJECT: %s", attached.getLocationURI()) );
			}
			break;
		default: 
			break;
		}
	}

	@Override
	public Runnable transactionCommited(final Collection<PropertyChangeEvent> events) {
		return new Runnable() {
			@Override
			public void run() { monitor(events); }
		};
	}

	public void monitor(final Collection<PropertyChangeEvent> events) {
		if (ProjectUsageIntegrityPlugin.getInstance().isProjectUsageIntegrityCheckerEnabled() ) {

			Logger log = MDLog.getPluginsLog();

			if ( between_pre_post_update ) {
				log.info( String.format("### TeamworkTransactionMonitor(%d events) between pre/post update", events.size()));
				return;
			}

			final List<UnlockedModificationInfo> canBeLockedModifications = new ArrayList<UnlockedModificationInfo>();
			final List<UnlockedModificationInfo> alreadyLockedModifications = new ArrayList<UnlockedModificationInfo>();
			for (PropertyChangeEvent event : events) {
				UnlockedModificationInfo change = unlockedModification(event.getPropertyName(), event.getOldValue(), event.getNewValue());
				if (change != null) {
					if (change.canBeLocked)
						canBeLockedModifications.add(change);
					else
						alreadyLockedModifications.add(change);
				}
			}

			if (canBeLockedModifications.isEmpty() && alreadyLockedModifications.isEmpty()) {
				log.info( String.format("### TeamworkTransactionMonitor(%d events) => invariants OK", events.size())); 
				return;
			}

			log.info( String.format("### TeamworkTransactionMonitor(%d events) => %d modifications of unlocked elements, %d modifications of elements locked by someone else", 
					events.size(), canBeLockedModifications.size(), alreadyLockedModifications.size()) );

			final GUILog guiLog = Application.getInstance().getGUILog();

			for (UnlockedModificationInfo change : canBeLockedModifications) {
				if (change.isDeletion)
					MDLog.getPluginsLog().log(Priority.WARN, change.result.getReason());
				else
					MDLog.getPluginsLog().log(Priority.WARN, change.result.getReason());
			}
			for (UnlockedModificationInfo change : alreadyLockedModifications) {
				if (change.isDeletion)
					MDLog.getPluginsLog().log(Priority.WARN, change.result.getReason());
				else
					MDLog.getPluginsLog().log(Priority.WARN, change.result.getReason());
			}
		}

	}

	public UnlockedModificationInfo unlockedModification( String property, Object oldValue, Object newValue ) {
		Logger log = MDLog.getPluginsLog();
		UnlockedModificationInfo verdict = null;
		if ("INSTANCE_DELETED".equals(property) && oldValue instanceof Element && newValue == null) {
			verdict = checkIfUnlocked( (Element) oldValue, true );
		} else if (oldValue == null && newValue instanceof Element) {
			Element e = (Element) newValue;
			SortedSet<String> containmentPropertyNames = MetamodelTransactionPropertyNameCache.getModifiablePropertyNamesForMetaclassOfElement(e);
			if (containmentPropertyNames.contains(property))
				verdict = checkIfUnlocked( e , false);
		} else if (oldValue instanceof Element && newValue == null) {
			Element e = (Element) oldValue;
			SortedSet<String> containmentPropertyNames = MetamodelTransactionPropertyNameCache.getModifiablePropertyNamesForMetaclassOfElement(e);
			if (containmentPropertyNames.contains(property))
				verdict = checkIfUnlocked( e , false);
		}
		if ( ApplicationEnvironment.isDeveloper() )
			log.info( String.format("### TeamworkEvent? %b : %s old=%s, new=%s",
					(verdict != null), property, getDescription(property, oldValue), getDescription(property, newValue)));

		return verdict;
	}

	public boolean isLockRelevant(ILockProjectService s, Element e) {
		return s.canBeLocked(e) || s.isLocked(e);
	}

	public Element getLockRelevantSelfOrAncestor(ILockProjectService s, Element e) {
		if (e == null)
			return null;

		if (isLockRelevant(s, e))
			return e;
		else
			return getLockRelevantSelfOrAncestor(s, e.getOwner());
	}

	public UnlockedModificationInfo checkIfUnlocked(Element e, boolean isDeletion) {
		Project p = Project.getProject(e);
		if (p == null) return null;

		ILockProjectService s = LockService.getLockService(p);
		if (s == null) return null;

		Element le = getLockRelevantSelfOrAncestor(s, e);
		if (le == null) return null;

		LockInfo info = s.getLockInfo(le);
		Logger log = MDLog.getPluginsLog();

		if (s.isLocked(le) && ! s.isLockedByMe(le) && info != null) {
			String message = (e != le) ? String.format("%s: %s locked by '%s' from lockable parent %s: %s",
					e.getHumanType(), getElementName(e), info.getUser(), le.getHumanType(), getElementName(le))
					: String.format("%s: %s locked by '%s'",
							e.getHumanType(), getElementName(e), info.getUser());
			if ( ApplicationEnvironment.isDeveloper() ) {
				log.info( String.format("### LOCKED: canBeLocked=%b, isLockedByMe=%b, isLocked=%b -- %s",
						s.canBeLocked(le), s.isLockedByMe(le), s.isLocked(le), message));
			}
			return new UnlockedModificationInfo(s, e, le, new ModelValidationResult(e, String.format("Illegal modification of %s", message)), false, isDeletion);
		}

		if (s.canBeLocked(le) && ! s.isLockedByMe(le) && info == null) {
			String message = (e != le) ? String.format("unlocked %s: %s (should have been locked from %s: %s)",
					e.getHumanType(), getElementName(e), le.getHumanType(), getElementName(le))
					: String.format("unlocked %s: %s (should have been locked)",
							e.getHumanType(), getElementName(e));
			if ( ApplicationEnvironment.isDeveloper() ) {
				log.info( String.format("### UNLOCKED: canBeLocked=%b, isLockedByMe=%b, isLocked=%b -- %s",
						s.canBeLocked(le), s.isLockedByMe(le), s.isLocked(le), message));
			}
			return new UnlockedModificationInfo(s, e, le, new ModelValidationResult(e, String.format("Illegal modification of %s", message)), true, isDeletion);
		}

		if ( ApplicationEnvironment.isDeveloper() ) {
			if (e != le)
				log.info( String.format("### N/A: canBeLocked=%b, isLockedByMe=%b, isLocked=%b, info.user=%s -- %s: %s {lockable parent %s: %s}",
						s.canBeLocked(le), s.isLockedByMe(le), s.isLocked(le), (info == null ? "<none>" : info.getUser()), 
						e.getHumanType(), getElementName(e),
						le.getHumanType(), getElementName(le)));
			else
				log.info( String.format("### N/A: canBeLocked=%b, isLockedByMe=%b, isLocked=%b, info.user=%s -- %s: %s",
						s.canBeLocked(le), s.isLockedByMe(le), s.isLocked(le), (info == null ? "<none>" : info.getUser()), 
						e.getHumanType(), getElementName(e)));
		}
		return null;
	}

	public boolean lock(List<UnlockedModificationInfo> unlockedModifications) {
		Logger log = MDLog.getPluginsLog();
		log.info(String.format("### Attempting to lock %d unlocked teamwork elements...", unlockedModifications.size()));

		List<UnlockedModificationInfo> changes = new ArrayList<UnlockedModificationInfo>();
		for(UnlockedModificationInfo unlockedModification: unlockedModifications) {
			boolean failed = false;
			if (unlockedModification.s.lockElements( java.util.Collections.singletonList( unlockedModification.lockable), true, null)) {
				if (unlockedModification.s.isLockedByMe(unlockedModification.lockable)) {
					log.info(String.format("### - Lock confirmed for %s ", getElementName(unlockedModification.lockable)));
					changes.add(unlockedModification);					
				} else {
					log.error(String.format("### - Unconfirmed lock for %s ", getElementName(unlockedModification.lockable)));
					failed = true;
				}
			} else {
				log.error(String.format("### - Could not lock %s ", getElementName(unlockedModification.lockable)));
				failed = true;
			}
			if (failed) {
				log.info(String.format("### => rollback; releasing %d locks", changes.size()));
				for (UnlockedModificationInfo change : changes) {
					if (change.s.unlockElements( java.util.Collections.singletonList( change.lockable), true, null)) {
						log.info(String.format("### -- rollback: unlocked %s ", getElementName(unlockedModification.lockable)));
					} else {
						log.error(String.format("### -- rollback: failed to unlock %s ", getElementName(unlockedModification.lockable)));
					}
				}
				return false;
			}
		}
		return true;
	}

	public static final int MAX_DESCRIPTION_LENGTH = 60;

	public String getDescription(String propertyName, Object o) {
		if (o == null)
			return "<null>";

		if (o instanceof Element)
			return getElementName((Element) o);

		String description = o.toString();
		if (description.length() < MAX_DESCRIPTION_LENGTH)
			return description;

		if ("ID" == propertyName)
			return description;

		return String.format("...(%d characters)", description.length());
	}

	public String getElementName(Element e) {
		String metaclassName = e.eClass().getName();
		String name = "";
		if (e instanceof NamedElement) {
			NamedElement ne = (NamedElement) e;
			name = ne.getName();
			if (name == null || name == "") name = "<unnamed>";
		}
		ProxyManager proxyManager = Project.getProject(e).getProxyManager();
		String suffix = (proxyManager.isElementProxy(e) ? "(proxy)" : "");
		return String.format("'%s'[%s@%s]%s", name, metaclassName, e.getID(), suffix);
	}
}