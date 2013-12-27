package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import com.nomagic.ci.persistence.local.ProjectState;

public interface MDAttachedProject {

	public SSCAEProjectDigest getDigest();

	public void setDigest(SSCAEProjectDigest digest);
	
	public void updateIndex(String index);
	
	// MD17.0.5
	public ProjectState getState();
	
	// MD17.0.5
	public void setState(ProjectState state);
	
}
