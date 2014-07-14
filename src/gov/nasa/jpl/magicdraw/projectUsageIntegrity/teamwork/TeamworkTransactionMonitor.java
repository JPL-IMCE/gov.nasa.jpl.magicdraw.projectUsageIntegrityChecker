package gov.nasa.jpl.magicdraw.projectUsageIntegrity.teamwork;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.jruby.util.unsafe.GeneratedUnsafe;

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
import com.nomagic.magicdraw.teamwork2.locks.LockService;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.actions.SelectInContainmentTreeRunnable;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.transaction.ModelValidationResult;
import com.nomagic.uml2.transaction.RollbackException;
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
public class TeamworkTransactionMonitor implements TransactionCommitListener, ProjectListener {

	static class UnlockedModificationInfo {
		public final ILockProjectService s;
		public final Element element;
		public final String elementPath;
		public final ModelValidationResult result;
		public UnlockedModificationInfo(ILockProjectService s, Element element, String elementPath, ModelValidationResult result) {
			this.s = s;
			this.element = element;
			this.elementPath = elementPath;
			this.result = result;
		}	
	}
	
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
			Logger log = MDLog.getPluginsLog();
			if ( ApplicationEnvironment.isDeveloper() )
				log.info( String.format("### TeamworkTransactionMonitor(%d events) between pre/post update? %b", events.size(), between_pre_post_update ));
			
			if ( between_pre_post_update )
				return;
			
			final List<UnlockedModificationInfo> unlockedModifications = new ArrayList<UnlockedModificationInfo>();
			for (PropertyChangeEvent event : events) {
				UnlockedModificationInfo unlockedModification = unlockedModification(event.getPropertyName(), event.getOldValue(), event.getNewValue());
				if (unlockedModification != null)
					unlockedModifications.add(unlockedModification);
			}
			if ( ApplicationEnvironment.isDeveloper() )
				log.info( String.format("### TeamworkTransactionMonitor(%d  events) => unlocked modifications: %d", events.size(), unlockedModifications.size()) );
			
			if (! unlockedModifications.isEmpty()) {
				String buff = String.format("Lock the %d elements?", unlockedModifications.size());
				for (UnlockedModificationInfo unlockedModification : unlockedModifications) {
					buff = buff + "\n" + unlockedModification.elementPath;
				}
				final String message = buff;
				
				final GUILog guiLog = Application.getInstance().getGUILog();
				guiLog.log( String.format("ERROR: Illegal modification of %d unlocked teamwork model elements!", unlockedModifications.size()) );
				
				switch (JOptionPane.showConfirmDialog(
						MDDialogParentProvider.getProvider().getDialogParent(),
						message, String.format("Illegal modifications of %d unlocked teamwork elements detected!", unlockedModifications.size()),
						JOptionPane.OK_CANCEL_OPTION)) {
				case JOptionPane.OK_OPTION: 
					if (lock(unlockedModifications))	return;
					break;
				default: break;
				}
				
				List<ModelValidationResult> results = new ArrayList<ModelValidationResult>();
				for (UnlockedModificationInfo unlockedModification : unlockedModifications) {
					String info = String.format("Modification of Unlocked Teamwork %s: %s", unlockedModification.element.getHumanType(), unlockedModification.elementPath);
					guiLog.addHyperlinkedText( String.format("=> <A>%s</A>\n", info), java.util.Collections.singletonMap( info, (Runnable) new SelectInContainmentTreeRunnable( unlockedModification.element ) ) );
					results.add(unlockedModification.result);
				}
				final Runnable r = new Runnable() {
					@Override
					public void run() {
						switch (JOptionPane.showConfirmDialog(
								MDDialogParentProvider.getProvider().getDialogParent(),
								message, 
								String.format("Illegal modifications of %d unlocked teamwork elements detected!", unlockedModifications.size()),
								JOptionPane.OK_CANCEL_OPTION )) {
					    case JOptionPane.OK_OPTION:
					    	  if (lock( unlockedModifications )) 
					    		  guiLog.log(String.format("Locked %d teamwork elements", unlockedModifications.size()));
					    	  else
   		  					  guiLog.log(String.format("*** ERROR *** Failed to lock %d teamwork elements", unlockedModifications.size())); 
					    	  break;
					    	  
					    default:
					    	  guiLog.log(String.format("Cancelled locking %d teamwork elements", unlockedModifications.size())); 
					    	  break;
						}
					}
				};
				guiLog.addHyperlinkedText( String.format("Consider: <A>%s</A>", message), java.util.Collections.singletonMap( message, r));
				
				RollbackException re = new RollbackException(results);

				// Since com.nomagic.uml2.transaction.RollbackException is a "checked" exception, 
				// the Java compiler does not allow writing:
				//
				// throw re;
				
				// Some techniques involve Java reflection to access sun.misc.Unsafe
				// However, Java 1.6 prevents accessing sun.misc.Unsafe in user code.
				// Using reflection may fail in some versions of Java.
				
//				try {
//					Class<?> unsafe = java.lang.Class.forName("sun.misc.Unsafe");
//					Field theUnsafe = unsafe.getDeclaredField("theUnsafe");
//					Method throwException = unsafe.getDeclaredMethod("throwException", Throwable.class);
//					theUnsafe.setAccessible(true);
//					Object u = theUnsafe.get(null);
//					throwException.invoke(u, re);
//				} catch (NoSuchFieldException e) {
//					log.error("NoSuchFieldException -- Unsafe.theUnsafe", e);
//					invariants_violated = true;
//					throw new RuntimeException(re);
//				} catch (SecurityException e) {
//					log.error("SecurityException", e);
//					invariants_violated = true;
//					throw new RuntimeException(re);
//				} catch (IllegalArgumentException e) {
//					log.error("IllegalArgumentException", e);
//					invariants_violated = true;
//					throw new RuntimeException(re);
//				} catch (IllegalAccessException e) {
//					log.error("IllegalAccessException", e);
//					invariants_violated = true;
//					throw new RuntimeException(re);
//				} catch (ClassNotFoundException e) {
//					log.error("ClassNotFoundException -- sun.misc.Unsafe", e);
//					invariants_violated = true;
//					throw new RuntimeException(re);
//				} catch (NoSuchMethodException e) {
//					log.error("NoSuchMethodException -- Unsafe.throwException(Throwable)", e);
//					invariants_violated = true;
//					throw new RuntimeException(re);
//				} catch (InvocationTargetException e) {
//					log.error("InvocationTargetException -- Unsafe.throwException(Throwable)", e);
//					invariants_violated = true;
//					throw new RuntimeException(re);
//				}
				
				// THis is a scala-like workaround thanks to JRuby!
				
				new GeneratedUnsafe().throwException(re);
			}
		}
		
		public UnlockedModificationInfo unlockedModification( String property, Object oldValue, Object newValue ) {
			Logger log = MDLog.getPluginsLog();
			UnlockedModificationInfo verdict = null;
			if ("INSTANCE_DELETED".equals(property) && oldValue instanceof Element && newValue == null) {
				verdict = checkIfUnlocked( (Element) oldValue );
			} else if ("owningElement".equals(property) && oldValue == null && newValue instanceof Element) {
				verdict = checkIfUnlocked( (Element) newValue );
			}
			if ( ApplicationEnvironment.isDeveloper() )
				log.info( String.format("### TeamworkEvent? %b : %s old=%s, new=%s",
						(verdict != null),
						property,
						(oldValue == null ? "null" : oldValue.toString()),
						(newValue == null ? "null" : newValue.toString())));
			return verdict;
		}
		
		public UnlockedModificationInfo checkIfUnlocked(Element e) {
			Project p = Project.getProject(e);
			if (p == null) return null;
			ILockProjectService s = LockService.getLockService(p);
			if (s == null) return null;
			if (s.canBeLocked(e) && ! s.isLockedByMe(e))
				return new UnlockedModificationInfo(s, e, getElementName(e), new ModelValidationResult(e, "Modification of unlocked teamwork element"));
			else
				return null;
		}
		
		public boolean lock(List<UnlockedModificationInfo> unlockedModifications) {
			boolean ok = true;
			for(UnlockedModificationInfo unlockedModification: unlockedModifications) {
				ok &= lock(unlockedModification.s, unlockedModification.element);
			}
			return ok;
		}
		
		public boolean lock(ILockProjectService s, Element element) {
			return s.lockElements( java.util.Collections.singletonList( element ), true, null );
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
