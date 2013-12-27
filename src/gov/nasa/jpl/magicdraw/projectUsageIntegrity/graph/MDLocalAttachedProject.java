package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.ci.persistence.local.ProjectState;
import com.nomagic.ci.persistence.local.spi.localproject.LocalAttachedProject;
import com.nomagic.magicdraw.core.Project;

public class MDLocalAttachedProject extends MDLocalProject implements MDAttachedProject {

	private SSCAEProjectDigest digest;

	private ProjectState state;

	public void setState(ProjectState state) {
		this.state = state;
	}

	public ProjectState getState() {
		return state;
	}

	public MDLocalAttachedProject() {
		super();
	}

	public static void configure(MDLocalAttachedProject that, @Nonnull Project rootProject, LocalAttachedProject p, String index) throws RemoteException {
		MDLocalProject.configure(that, rootProject, p, index);
		that.setState(p.getProjectState());
		that.setRoot(false);
		that.refresh(p);
	}

	@Override
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);

		assert (p instanceof LocalAttachedProject);
		LocalAttachedProject lap = (LocalAttachedProject) p;
		this.setState(lap.getProjectState());
		
		this.setLabel(String.format("[%s] '%s {ID=%s, loc=%s, md5=%s}", 
				this.getIndex(), this.getName(), this.getProjectID(), this.getLocation(), this.getMD5checksum()));

		this.setMDInfo(String.format("{%s, %s, %s, %s, %s}", 
					asBooleanFlag("isNew", p.isNew()), 
					asBooleanFlag("isReadOnly", p.isReadOnly()),
					asBooleanFlag("isManuallyReachable", lap.isManuallyReachable()),
					asBooleanFlag("isAvailable", lap.isProjectAvailable()),
					asBooleanFlag("isLoaded", lap.isLoaded())));
	}

	public SSCAEProjectDigest getDigest() {
		return digest;
	}

	public void setDigest(SSCAEProjectDigest digest) {
		this.digest = digest;
	}

	@Override
	public void updateIndex(String index) {		
		setLabel("[L" + index + "]" + getLabel().substring(2 + getIndex().length()));
		setIndex("L" + index);
	}
}
