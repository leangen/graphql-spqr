package io.leangen.graphql.query.relay.generic;

import io.leangen.graphql.query.relay.Edge;

/**
 * Created by bojan.tomic on 5/16/16.
 */
public class GenericEdge<N> implements Edge<N> {

    private N node;
    private String cursor;

    public GenericEdge(N node, String cursor) {
        this.node = node;
        this.cursor = cursor;
    }

    @Override
    public String getCursor() {
        return cursor;
    }

    @Override
    public N getNode() {
        return node;
    }
}
