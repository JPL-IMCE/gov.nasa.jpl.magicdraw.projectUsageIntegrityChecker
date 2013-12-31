package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

public class ElementPathSegment {
	public ElementPathSegment(String elementID, String segment) {
		this.elementID = elementID;
		this.segment = segment;
	}
	public final String elementID;
	public final String segment;
	
	@Override
	public String toString() {
		return segment;
	}
}