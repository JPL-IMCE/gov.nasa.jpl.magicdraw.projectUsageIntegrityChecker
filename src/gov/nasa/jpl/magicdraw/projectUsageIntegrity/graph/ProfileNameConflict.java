package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

public class ProfileNameConflict {

	private String p1;
	private String p1ID;
	private String p2;
	private String p2ID;
	
	public ProfileNameConflict() {
		this.p1 = "";
		this.p1ID = "";
		this.p2 = "";
		this.p2ID = "";
	}

	public ProfileNameConflict(String p1, String p1ID, String p2, String p2ID) {
		this.p1 = p1;
		this.p1ID = p1ID;
		this.p2 = p2;
		this.p2ID = p2ID;
	}

	public String getP1() {
		return p1;
	}

	public void setP1(String p1) {
		this.p1 = p1;
	}
	
	public String getP1ID() {
		return p1ID;
	}

	public void setP1ID(String p1ID) {
		this.p1ID = p1ID;
	}

	public String getP2() {
		return p2;
	}

	public void setP2(String p2) {
		this.p2 = p2;
	}
	
	public String getP2ID() {
		return p2ID;
	}

	public void setP2ID(String p2ID) {
		this.p2ID = p2ID;
	}
}
