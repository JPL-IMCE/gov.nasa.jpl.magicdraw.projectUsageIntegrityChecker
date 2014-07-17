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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.transactions;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLPackage;

/**
 * @author Nicolas.F.Rouquette@jpl.nasa.gov
 * @see https://support.nomagic.com/browse/MDUMLCS-13731
 *
 * Verify that every MD TransactionCommit involves changes that satisfy the following rules:
 * 1) deletion or modification of unlocked elements should be a consequence of updating the in memory model based on changes from the teamwork server
 * 2) outside of the context of a teamwork update or version change, MD should veto any transaction that results in modifying or deleting an unlocked element
 *
 * MagicDraw transaction commits are visible in terms of java beans property change events.
 * The property name for such events can be:
 * - INSTANCE_CREATED old=null, new=<new element>
 * - INSTANCE_DELETED old=<deleted element>, new=null
 * - <uml metaclass property name> old=x, new=y
 * - something else
 * 
 * This class builds a one-time cache of all <uml metaclass property name>.
 * In particular, it distinguishes between composite & non-composite uml metaclass property names.
 */
public class MetamodelTransactionPropertyNameCache {


	public static Comparator<EClass> ECLASS_COMPARATOR = new Comparator<EClass>() {

		@Override
		public int compare(EClass ec1, EClass ec2) {
			return ec1.getName().compareTo(ec2.getName());
		}

	};

	protected static final SortedMap<EClass, SortedSet<String>> TRANSACTION_MODIFIABLE_PROPERTY_NAMES_BY_METACLASS = new TreeMap<EClass, SortedSet<String>>(ECLASS_COMPARATOR);

	public static String describeModifiablePropertyNamesForMetaclass(EClass mc) {
		SortedSet<String> modifiablePropertyNames = TRANSACTION_MODIFIABLE_PROPERTY_NAMES_BY_METACLASS.get(mc);
		if (null == modifiablePropertyNames)
			return null;
		StringBuffer buff = new StringBuffer();
		for (String modifiablePropertyName : modifiablePropertyNames) {
			buff.append(String.format(" '%s'", modifiablePropertyName));
		}
		return buff.toString();
	}

	public static SortedSet<String> getModifiablePropertyNamesForMetaclassOfElement(Element e) {
		if (e == null)
			return null;

		EClass ec = e.eClass();
		if (ec == null)
			return null;

		SortedSet<String> containmentPropertyNames = TRANSACTION_MODIFIABLE_PROPERTY_NAMES_BY_METACLASS.get(ec);
		return containmentPropertyNames;
	}

	public static void initialize() {
		Logger log = MDLog.getPluginsLog();

		EPackage eUML = UMLPackage.eINSTANCE;
		SortedSet<EClass> metaclasses = new TreeSet<EClass>(ECLASS_COMPARATOR);
		Map<EClass, SortedSet<EClass>> metaclass2specializationChildrenIncludingSelf = new HashMap<EClass, SortedSet<EClass>>();

		for (TreeIterator<EObject> i = eUML.eAllContents(); i.hasNext();) {
			EObject e = i.next();
			if (e instanceof EClass) {
				EClass mc = (EClass) e;
				metaclasses.add(mc);

				TRANSACTION_MODIFIABLE_PROPERTY_NAMES_BY_METACLASS.put(mc, new TreeSet<String>());
				SortedSet<EClass> childrenIncludingSelf = new TreeSet<EClass>(ECLASS_COMPARATOR);
				childrenIncludingSelf.add(mc);
				metaclass2specializationChildrenIncludingSelf.put(mc, childrenIncludingSelf);
			}
		}

		for (EClass mc : metaclasses) {
			for (EClass parent : mc.getEAllSuperTypes()) {
				if (metaclasses.contains(parent)) {
					metaclass2specializationChildrenIncludingSelf.get(parent).add(mc);
				}
			}
		}

		for (EClass mc : metaclasses) {
			for (EStructuralFeature f : mc.getEAllStructuralFeatures()) {
				if (!(f instanceof EReference))
					continue;

				EReference p = (EReference) f;

				if (p.isTransient() && !p.isDerived() && !p.isVolatile())
					continue;

				if (p.isContainment()) {
					boolean added = TRANSACTION_MODIFIABLE_PROPERTY_NAMES_BY_METACLASS.get(mc).add(p.getName());
					if ( ApplicationEnvironment.isDeveloper() ) {
						log.debug(String.format("### %s => containment %s (new? %b)", mc.getName(), p.getName(), added));
					}
				} else if (p.isContainer()) {
					SortedSet<EClass> childrenIncludingSelf = metaclass2specializationChildrenIncludingSelf.get((EClass) p.getEType());
					if (childrenIncludingSelf != null) {
						for (EClass container : childrenIncludingSelf) {
							boolean added = TRANSACTION_MODIFIABLE_PROPERTY_NAMES_BY_METACLASS.get(container).add(p.getName());
							if ( ApplicationEnvironment.isDeveloper() ) {
								log.debug(String.format("### (from %s): %s => container %s (new? %b)", mc.getName(), container.getName(), p.getName(), added));
							}
						}
					}
				} else {
					boolean added = TRANSACTION_MODIFIABLE_PROPERTY_NAMES_BY_METACLASS.get(mc).add(p.getName());
					if ( ApplicationEnvironment.isDeveloper() ) {
						log.debug(String.format("### %s => reference %s (new? %b)", mc.getName(), p.getName(), added));
					}
				}
			}
		}

		if ( ApplicationEnvironment.isDeveloper() ) {
			for (EClass mc : metaclasses) {
				log.debug(String.format("### MODIFIABLE PROPERTY NAMES for %s:\n%s", mc.getName(), describeModifiablePropertyNamesForMetaclass(mc)));
			}
		}
	}
}
