package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;

/**
 * @see org.jgrapht.ext.DOTExporter
 * @author Nicolas Rouquette
 */
public abstract class DOTExporterWithLegend<V, E>  {

	protected VertexNameProvider<V> vertexIDProvider;
	protected VertexNameProvider<V> vertexLabelProvider;
	protected EdgeNameProvider<E> edgeLabelProvider;
	protected ComponentAttributeProvider<V> vertexAttributeProvider;
	protected ComponentAttributeProvider<E> edgeAttributeProvider;

	public DOTExporterWithLegend(
			VertexNameProvider<V> vertexIDProvider,
			VertexNameProvider<V> vertexLabelProvider,
			EdgeNameProvider<E> edgeLabelProvider,
			ComponentAttributeProvider<V> vertexAttributeProvider,
			ComponentAttributeProvider<E> edgeAttributeProvider) {
		this.vertexIDProvider = vertexIDProvider;
		this.vertexLabelProvider = vertexLabelProvider;
		this.edgeLabelProvider = edgeLabelProvider;
		this.vertexAttributeProvider = vertexAttributeProvider;
		this.edgeAttributeProvider = edgeAttributeProvider;
	}

	public void export(Writer writer, Graph<V, E> g) {
		PrintWriter out = new PrintWriter(writer);
		String indent = "  ";
		String connector;

		out.println("digraph G {");
		connector = " -> ";

		for (V v : g.vertexSet()) {
			out.print(indent + getVertexID(v));

			String labelName = null;
			if (vertexLabelProvider != null) {
				labelName = vertexLabelProvider.getVertexName(v);
			}
			Map<String, String> attributes = null;
			if (vertexAttributeProvider != null) {
				attributes = vertexAttributeProvider.getComponentAttributes(v);
			}
			renderAttributes(out, labelName, attributes);

			out.println(";");
		}

		for (E e : g.edgeSet()) {
			String source = getVertexID(g.getEdgeSource(e));
			String target = getVertexID(g.getEdgeTarget(e));

			out.print(indent + source + connector + target);

			String labelName = null;
			if (edgeLabelProvider != null) {
				labelName = edgeLabelProvider.getEdgeName(e);
			}
			Map<String, String> attributes = null;
			if (edgeAttributeProvider != null) {
				attributes = edgeAttributeProvider.getComponentAttributes(e);
			}
			renderAttributes(out, labelName, attributes);

			out.println(";");
		}

		legend(out);

		out.println("}");
		out.flush();
	}

	protected abstract void legend(PrintWriter out);

	protected void renderAttributes(
			PrintWriter out,
			String labelName,
			Map<String, String> attributes)
	{
		if ((labelName == null) && (attributes == null)) {
			return;
		}
		out.print(" [ ");
		if ((labelName == null) && (attributes != null)) {
			labelName = attributes.get("label");
		}
		if (labelName != null) {
			out.print("label=\"" + labelName + "\" ");
		}
		if (attributes != null) {
			for (Map.Entry<String, String> entry : attributes.entrySet()) {
				String name = entry.getKey();
				if (name.equals("label")) {
					// already handled by special case above
					continue;
				}
				out.print(name + "=\"" + entry.getValue() + "\" ");
			}
		}
		out.print("]");
	}

	/**
	 * Return a valid vertex ID (with respect to the GraphViz' dot language definition as
	 * described in http://www.graphviz.org/doc/info/lang.html Quoted from above
	 * mentioned source: An ID is valid if it meets one of the following
	 * criteria:
	 *
	 * <ul>
	 * <li>any string of alphabetic characters, underscores or digits, not
	 * beginning with a digit;
	 * <li>a number [-]?(.[0-9]+ | [0-9]+(.[0-9]*)? );
	 * <li>any double-quoted string ("...") possibly containing escaped quotes
	 * (\");
	 * <li>an HTML string (<...>).
	 * </ul>
	 *
	 * @throws RuntimeException if the given <code>vertexIDProvider</code>
	 * didn't generate a valid vertex ID.
	 */
	protected String getVertexID(V v)
	{
		// TODO jvs 28-Jun-2008:  possible optimizations here are
		// (a) only validate once per vertex
		// (b) compile regex patterns

		// use the associated id provider for an ID of the given vertex
		String idCandidate = vertexIDProvider.getVertexName(v);

		// now test that this is a valid ID
		boolean isAlphaDig = idCandidate.matches("[a-zA-Z]+([\\w_]*)?");
		boolean isDoubleQuoted = idCandidate.matches("\".*\"");
		boolean isDotNumber =
				idCandidate.matches("[-]?([.][0-9]+|[0-9]+([.][0-9]*)?)");
		boolean isHTML = idCandidate.matches("<.*>");

		if (isAlphaDig || isDotNumber || isDoubleQuoted || isHTML) {
			return idCandidate;
		}

		throw new RuntimeException(
				"Generated id '" + idCandidate + "'for vertex '" + v
				+ "' is not valid with respect to the GraphViz dot language");
	}
}