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

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.ProjectUsageIntegrityPlugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import org.eclipse.emf.common.util.URI;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.local.spi.localproject.ILocalProjectInternal;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.utils.MDLog;

public abstract class MDLocalProject extends MDAbstractProject {

	private boolean isInstallRootRelative;
	private URI location;
	private String label;
	private String md5checksum;
	private boolean isMissing;
	private boolean isRoot;
	private boolean isTemplate;
	
	private boolean hasTeamworkProjectID;
	
	public MDLocalProject() {}
	
	public boolean isInstallRootRelative() {
		return isInstallRootRelative;
	}

	public void setInstallRootRelative(boolean isInstallRootRelative) {
		this.isInstallRootRelative = isInstallRootRelative;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getMD5checksum() {
		return md5checksum;
	}

	public void setMD5checksum(String md5checksum) {
		this.md5checksum = md5checksum;
	}

	public boolean isMissing() {
		return isMissing;
	}

	public void setMissing(boolean isMissing) {
		this.isMissing = isMissing;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	public boolean isTemplate() {
		return isTemplate;
	}

	/**
	 * See: https://support.nomagic.com/browse/MDUMLCS-10009
	 * @return true if the project ID matches one of the defined teamwork ID suffixes.
	 */
	public boolean hasTeamworkProjectID() {
		return hasTeamworkProjectID;
	}

	public URI getLocation() { return this.location; }

	
	public void setLocation(URI location) {
		this.location = location;
	}

	public void setHasTeamworkProjectID(boolean hasTeamworkProjectID) {
		this.hasTeamworkProjectID = hasTeamworkProjectID;
	}

	public void setTemplate(boolean isTemplate) {
		this.isTemplate = isTemplate;
	}

	public static void configure(MDLocalProject that, @Nonnull Project rootProject, ILocalProjectInternal p, String index) throws RemoteException {
		MDAbstractProject.configure(that, p, "L" + index);

		// workaround for https://support.nomagic.com/browse/MDUMLCS-10009
		String projectID = that.getProjectID();
		for (String teamworkSuffix : ProjectUsageIntegrityPlugin.getInstance().MDTeamworkProjectIDSuffixes) {
			if (projectID.endsWith(teamworkSuffix)) {
				that.setHasTeamworkProjectID(true);
				break;
			}
		}
		
		that.refresh(p);
	}

	
	@Override
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);
		
		this.setLocation(SSCAEProjectUsageGraph.getMDRelativeProjectURI(p));
		this.setInstallRootRelative(SSCAEProjectUsageGraph.isMDRelativeURI(this.getLocation()));
		this.setTemplate(this.isInstallRootRelative() && "templates".equals(this.getLocation().segment(0)));
		
		ProjectUsageIntegrityPlugin plugin = ProjectUsageIntegrityPlugin.getInstance();
		assert (p instanceof ILocalProjectInternal);
		ILocalProjectInternal lp = (ILocalProjectInternal) p;

		this.setTemplate(false);
		String md5result = "<not available>";
		String localFile = p.getLocationURI().toFileString();
		boolean missing = true;
		try {
			md5result = SSCAEProjectUsageGraph.getMD5FromFile(localFile);
			missing = false;
		} catch (FileNotFoundException e) {
			MDLog.getPluginsLog().error(String.format("%s: MDLocalProject(md5 computation) for project '%s': FileNotFound: %s",
					plugin.getPluginName(),
					p.getName(), localFile));
			md5result = "<FileNotFound>";
		} catch (IOException e) {
			MDLog.getPluginsLog().error(String.format("%s: MDLocalProject(md5 computation) for project '%s': IOException: %s",
					ProjectUsageIntegrityPlugin.getInstance().getPluginName(),
					p.getName(), localFile));
			md5result = "<IOException>";
		} finally {
			this.setMD5checksum(md5result);
			this.setMissing(missing);
		}
	}
	
	public boolean isProjectMissing() { return this.isMissing; }

	public boolean isRootProject() { return this.isRoot; }

	public boolean isLocalTemplate() { return this.isTemplate; }

	public String toString() { return this.label; }
}