package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.IProject;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.teamwork.application.storage.ITeamworkProject;
import com.nomagic.magicdraw.teamwork.application.storage.TeamworkPrimaryProject;

public class MDTeamworkPrimaryProject extends MDTeamworkProject {

	public MDTeamworkPrimaryProject() {
		super();
	}
	
	public static void configure(MDTeamworkProject that, @Nonnull Project rootProject, ITeamworkProject p, String index) throws RemoteException {
		MDTeamworkProject.configure(that, rootProject, p, index);
		that.setRoot(true);
		that.refresh(p);
		that.setVersion("<not computed for a teamwork primary project1>");
		that.setFullVersion("<not computed for a teamwork primary project2>");
		that.setAnonymizedVersion("<not computed for a teamwork primary project3>");
	}
	

	@Override
	public void refresh(IProject p) throws RemoteException {
		super.refresh(p);
		
		assert (p instanceof TeamworkPrimaryProject);
		TeamworkPrimaryProject tpp = (TeamworkPrimaryProject) p;
		
		this.setMDInfo(String.format("{%s, %s, %s, %s, %s, %s}", 
					asBooleanFlag("isNew", p.isNew()), 
					asBooleanFlag("isReadOnly", p.isReadOnly()),
					asBooleanFlag("isHistorical", tpp.isHistorical()),
					asBooleanFlag("isAvailable", tpp.isProjectAvailable()),
					asBooleanFlag("isReadFromTeamwork", tpp.isReadFromTeamwork()),
					asBooleanFlag("isUpToDate", tpp.isUpToDate())));
	}
}
