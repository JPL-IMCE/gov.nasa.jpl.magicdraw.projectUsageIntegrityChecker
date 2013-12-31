package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

public class URIConflict {

	private String p1;
	private String p1ID;
	private String p2;
	private String p2ID;
	private String URI;
	
	public URIConflict() {
		this.p1 = "";
		this.p1ID = "";
		this.p2 = "";
		this.p2ID = "";
		this.URI = "";
	}

	public URIConflict(String p1, String p1ID, String p2, String p2ID, String URI) {
		this.p1 = p1;
		this.p1ID = p1ID;
		this.p2 = p2;
		this.p2ID = p2ID;
		this.URI = URI;
	}
	
	public String getURI() {
		return URI;
	}
	
	public void setURI(String URI) {
		this.URI = URI;
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
