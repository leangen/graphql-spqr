package io.leangen.graphql.query.relay;

/**
 * Created by bojan.tomic on 4/6/16.
 */
public interface Page<N> {
	Iterable<Edge<N>> getEdges();
	PageInfo getPageInfo();
}