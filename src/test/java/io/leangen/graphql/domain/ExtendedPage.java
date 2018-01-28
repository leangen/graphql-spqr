package io.leangen.graphql.domain;

import java.util.List;

import graphql.relay.Edge;
import graphql.relay.PageInfo;
import io.leangen.graphql.execution.relay.Connection;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;

public class ExtendedPage<N> implements Connection<Edge<N>> {

    private final List<Edge<N>> edges;
    private final PageInfo pageInfo;
    private final long totalCount;

    public ExtendedPage(List<N> nodes, long totalCount, long offset) {
        Page<N> page = PageFactory.createOffsetBasedPage(nodes, totalCount, offset);
        this.edges = page.getEdges();
        this.pageInfo = page.getPageInfo();
        this.totalCount = totalCount;
    }

    @Override
    public List<Edge<N>> getEdges() {
        return edges;
    }

    @Override
    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public long getTotalCount() {
        return totalCount;
    }
}
