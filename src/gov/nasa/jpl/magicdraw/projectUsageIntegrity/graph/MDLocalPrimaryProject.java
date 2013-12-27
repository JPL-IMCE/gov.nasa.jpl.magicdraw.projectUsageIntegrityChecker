package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.local.spi.localproject.ILocalProjectInternal;
import com.nomagic.ci.persistence.local.spi.localproject.LocalPrimaryProject;
import com.nomagic.magicdraw.core.Project;

public class MDLocalPrimaryProject extends MDLocalProject {

	public MDLocalPrimaryProject() {
		super();
	}
	
	public static void configure(MDLocalProject that, @Nonnull Project rootProject, ILocalProjectInternal p, String index) throws RemoteException {
		MDLocalProject.configure(that, rootProject, p, index);
		that.setRoot(true);
		that.refresh(p);
	}
	
	@Override 
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);
		assert (p instanceof LocalPrimaryProject);
		LocalPrimaryProject lpp = (LocalPrimaryProject) p;
		
		String id = this.getProjectID();
		if (this.isTemplate()) {
			id = "<template>";
		} 
		this.setLabel(String.format("[%s] '%s {ID=%s, loc=%s}", 
				this.getIndex(), this.getName(), id, this.getLocation()));
		
		this.setMDInfo(String.format("{%s, %s}", 
					asBooleanFlag("isNew", p.isNew()), 
					asBooleanFlag("isReadOnly", p.isReadOnly())));
	} 
}
