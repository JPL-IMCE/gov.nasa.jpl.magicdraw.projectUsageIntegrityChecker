package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.teamwork.application.storage.ITeamworkProject;

public class MDTeamworkPrimaryProject extends MDTeamworkProject {

	public MDTeamworkPrimaryProject() {
		super();
	}
	
	public static void configure(MDTeamworkProject that, @Nonnull Project rootProject, ITeamworkProject p, String index) throws RemoteException {
		MDTeamworkProject.configure(that, rootProject, p, index);
		
		that.setVersion("<not computed for a teamwork primary project1>");
		that.setFullVersion("<not computed for a teamwork primary project2>");
		that.setAnonymizedVersion("<not computed for a teamwork primary project3>");
	}
}
