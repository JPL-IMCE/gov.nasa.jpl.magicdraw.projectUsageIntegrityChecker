package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.versioning.IVersionDescriptor;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.magicdraw.teamwork.application.storage.ITeamworkProject;

public class MDTeamworkAttachedProject extends MDTeamworkProject implements MDAttachedProject  {

	private SSCAEProjectDigest digest;
	
	public MDTeamworkAttachedProject() {
		super();
	}
	
	public static void configure(MDTeamworkProject that, @Nonnull Project rootProject, ITeamworkProject p, String index) throws RemoteException {
		MDTeamworkProject.configure(that, rootProject, p, index);
		
		final @Nonnull IVersionDescriptor version = ProjectUtilities.getVersion(p);
		that.setVersion(version.getName());

		final String teamworkRemoteId = ProjectUtilities.getTeamworkRemoteId(p);
		final @Nonnull ProjectDescriptor descriptor = TeamworkUtils.getRemoteProjectDescriptor(teamworkRemoteId);
		
		if (null == descriptor) {
			that.setFullVersion("[server not available]#" + that.getVersion());
		} else {
			that.setFullVersion(ProjectDescriptorsFactory.getProjectFullPath(descriptor.getURI()) + "#" + that.getVersion());
		}
		that.setAnonymizedVersion(String.format("%s#%s", that.getIndex(), that.getVersion()));

	}

	public SSCAEProjectDigest getDigest() {
		return digest;
	}

	public void setDigest(SSCAEProjectDigest digest) {
		this.digest = digest;
	}
	
	@Override
	public void updateIndex(String index) {
		setLabel("[T" + index + "]" + getLabel().substring(2 + getIndex().length()));
		setIndex("T" + index);
	}
}
