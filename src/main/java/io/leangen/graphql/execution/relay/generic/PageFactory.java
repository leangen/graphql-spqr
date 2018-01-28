package io.leangen.graphql.execution.relay.generic;

import java.util.ArrayList;
import java.util.List;

import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultConnectionCursor;
import graphql.relay.DefaultEdge;
import graphql.relay.DefaultPageInfo;
import graphql.relay.Edge;
import graphql.relay.PageInfo;
import io.leangen.graphql.execution.relay.CursorProvider;
import io.leangen.graphql.execution.relay.Page;

/**
 * Created by bojan.tomic on 2/19/17.
 */
@SuppressWarnings("WeakerAccess")
public class PageFactory {

    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long count, long offset) {
        return createOffsetBasedPage(nodes, offset, offset + nodes.size() < count, offset > 0 && count > 0);
    }

    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long offset, boolean hasNextPage, boolean hasPreviousPage) {
        return createPage(nodes, (node, index) -> new DefaultConnectionCursor(Long.toString(offset + index + 1)), hasNextPage, hasPreviousPage);
    }

    public static <N> Page<N> createPage(List<N> nodes, CursorProvider<N> cursorProvider, boolean hasNextPage, boolean hasPreviousPage) {
        List<Edge<N>> edges = createEdges(nodes, cursorProvider);
        return new GenericPage<>(edges, createPageInfo(edges, hasNextPage, hasPreviousPage));
    }

    public static <N> List<Edge<N>> createEdges(List<N> nodes, CursorProvider<N> cursorProvider) {
        List<Edge<N>> edges = new ArrayList<>(nodes.size());
        int index = 0;
        for (N node : nodes) {
            edges.add(new DefaultEdge<>(node, cursorProvider.createCursor(node, index++)));
        }
        return edges;
    }

    public static <N> PageInfo createPageInfo(List<Edge<N>> edges, boolean hasNextPage, boolean hasPreviousPage) {
        ConnectionCursor firstCursor = null;
        ConnectionCursor lastCursor = null;
        if (!edges.isEmpty()) {
            firstCursor = edges.get(0).getCursor();
            lastCursor = edges.get(edges.size() - 1).getCursor();
        }
        return new DefaultPageInfo(firstCursor, lastCursor, hasPreviousPage, hasNextPage);
    }
}
