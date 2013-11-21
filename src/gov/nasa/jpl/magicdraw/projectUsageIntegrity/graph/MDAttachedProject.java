package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

public interface MDAttachedProject {

	public SSCAEProjectDigest getDigest();

	public void setDigest(SSCAEProjectDigest digest);
	
	public void updateIndex(String index);
}
