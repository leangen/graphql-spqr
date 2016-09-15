package io.leangen.graphql.query.relay.generic;

import io.leangen.graphql.query.relay.Edge;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.query.relay.PageInfo;

import java.util.List;

/**
 * Created by bojan.tomic on 5/16/16.
 */
public class GenericPage<N> implements Page<N> {

    private List<Edge<N>> edges;
    private PageInfo pageInfo;

    public GenericPage(List<Edge<N>> edgdes, PageInfo<N> pageInfo) {
        this.edges = edgdes;
        this.pageInfo = pageInfo;
    }

    @Override
    public Iterable<Edge<N>> getEdges() {
        return edges;
    }

    @Override
    public PageInfo getPageInfo() {
        return pageInfo;
    }
}
