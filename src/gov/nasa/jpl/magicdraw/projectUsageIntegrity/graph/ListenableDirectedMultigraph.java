package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.DirectedMultigraph;

public class ListenableDirectedMultigraph<V, E>
extends DefaultListenableGraph<V, E>
implements DirectedGraph<V, E>
{
	private static final long serialVersionUID = 1L;

	ListenableDirectedMultigraph(Class<E> edgeClass) {
		super(new DirectedMultigraph<V, E>(edgeClass));
	}
}