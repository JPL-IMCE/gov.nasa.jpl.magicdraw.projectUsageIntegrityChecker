/*
 *
 * License Terms
 *
 * Copyright (c) 2013-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.String;
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
			String label,
			Map<String, String> attributes)
	{
		if ((label == null) && (attributes == null)) {
			return;
		}
		out.print(" [ ");
		if ((label == null) && (attributes != null)) {
			label = attributes.get("label");
		}
		if (label != null) {
			if (label.startsWith("<") && label.endsWith(">")) {
				out.print("label=" + label + " ");
			} else {
				out.print("label=\"" + label + "\" ");
			}
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