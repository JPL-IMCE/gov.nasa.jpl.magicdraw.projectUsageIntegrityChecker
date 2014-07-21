package gov.nasa.jpl.magicdraw.projectUsageIntegrity.expression;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph.ElementPathSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jmi.reflect.RefObject;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.nomagic.ci.persistence.IAttachedProject;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.proxy.ProxyManager;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.actions.SelectInContainmentTreeRunnable;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.reflect.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

public class ElementPath implements Expression {

	@Override
	public Object getValue(RefObject ref) {
		if (!(ref instanceof Element))
			return "";

		Element e = (Element) ref;
		StringBuffer buff = new StringBuffer();
		boolean first = true;
		for (ElementPathSegment path : getElementPath(e)) {
			if (first) {
				first = false;
			} else {
				buff.append(".");
			}
			buff.append(path);
		}
		return buff.toString();
	}

	public static void showElementPath(Project p, GUILog glog, String first, String others, List<ElementPathSegment> path) {
		boolean isFirst = true;
		for (ElementPathSegment segment : path) {
			String info = segment.toString();
			BaseElement e = p.getElementByID(segment.elementID);
			if (isFirst) {
				glog.addHyperlinkedText(String.format(first, info), Collections.singletonMap(info, (Runnable) new SelectInContainmentTreeRunnable(e)));
				isFirst = false;
			} else {
				glog.addHyperlinkedText(String.format(others, info), Collections.singletonMap(info, (Runnable) new SelectInContainmentTreeRunnable(e)));
			}
		}
	}
	
	public static List<ElementPathSegment> getElementPath(Element e) {
		if (e == null)
			return Collections.<ElementPathSegment>emptyList();

		Package eRoot = Project.getProject(e).getModel();
		final IAttachedProject attachedProject = ProjectUtilities.getAttachedProject(e);
		if (null != attachedProject) {
			for (Package sharedPkg : ProjectUtilities.getSharedPackages(attachedProject)) {
				if (sharedPkg.equals(e) || ModelHelper.isParentOf((BaseElement) sharedPkg, (BaseElement) e)) {
					eRoot = sharedPkg;
					break;
				}
			}
		}

		return getElementPathFrom(eRoot, e);
	}

	public static List<ElementPathSegment> getElementPathFrom(Element top, Element e) {
		return getElementPathFrom(top, e, new ArrayList<ElementPathSegment>());
	}

	public static List<ElementPathSegment> getElementPathFrom(Element top, Element e, List<ElementPathSegment> path) {
		if (top.equals(e)) {
			path.add(0, getElementNameSegment(e));
			return path;
		}

		EStructuralFeature f = e.eContainingFeature();
		if (f == null)
			return path;
		
		EObject c = e.eContainer();
		assert (c instanceof Element);
		Element ce = (Element) c;

		path.add(0, getElementFeatureSegment(f, e));
		return getElementPathFrom(top, ce, path);
	}

	public static ElementPathSegment getElementFeatureSegment(EStructuralFeature f, Element e) {
		return new ElementPathSegment(e.getID(), String.format("%s=%s", f.getName(), getElementName(e)));
	}
	
	public static ElementPathSegment getElementNameSegment(Element e) {
		return new ElementPathSegment(e.getID(), getElementName(e));
	}
	
	public static String getElementName(Element e) {
		String metaclassName = e.eClass().getName();
		String name = "\"\"";
		if (e instanceof NamedElement) {
			NamedElement ne = (NamedElement) e;
			if (ne.getName() != null) {
				name = ne.getName();
			}
		}
	
		Project p = Project.getProject(e);
		ProxyManager proxyManager = p.getProxyManager();
		String suffix = proxyManager.isElementProxy(e) ? "(proxy)" : "";
		
		String info = String.format("'%s'[%s@%s]%s", name, metaclassName, e.getID(), suffix);
		
		EStructuralFeature f = e.eContainingFeature();
		if ((e instanceof InstanceSpecification) && (f != null) && "appliedStereotypeInstance".equals(f.getName())) {
			InstanceSpecification asi = (InstanceSpecification) e;
			info = info + "&lt;&lt;";
			boolean isFirst = true;
			for (Classifier s : asi.getClassifier()) {
				if (isFirst) {
					isFirst = false;
				} else {
					info = info + ",";
				}
				info = info + s.getQualifiedName();
			}
			info = info + "&gt;&gt;";
		}
		
		return info;
	}
}
