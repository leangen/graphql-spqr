package io.leangen.graphql.execution.relay.generic;

import java.util.List;

import io.leangen.graphql.execution.relay.Edge;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.PageInfo;

/**
 * Created by bojan.tomic on 5/16/16.
 */
public class GenericPage<N> implements Page<N> {

    private List<Edge<N>> edges;
    private PageInfo pageInfo;

    @SuppressWarnings("WeakerAccess")
    public GenericPage(List<Edge<N>> edges, PageInfo<N> pageInfo) {
        this.edges = edges;
        this.pageInfo = pageInfo;
    }

    @Override
    public List<Edge<N>> getEdges() {
        return edges;
    }

    @Override
    public PageInfo getPageInfo() {
        return pageInfo;
    }
}
