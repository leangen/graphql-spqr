package io.leangen.graphql.query.relay.generic;

import java.util.ArrayList;
import java.util.List;

import io.leangen.graphql.query.relay.CursorProvider;
import io.leangen.graphql.query.relay.Edge;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.query.relay.PageInfo;

/**
 * Created by bojan.tomic on 2/19/17.
 */
public class PageFactory {

    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long count, long offset) {
        return createOffsetBasedPage(nodes, offset, offset + nodes.size() < count, offset > 0 && count > 0);
    }

    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long offset, boolean hasNextPage, boolean hasPreviousPage) {
        return createPage(nodes, (node, index) -> Long.toString(offset + index + 1), hasNextPage, hasPreviousPage);
    }

    public static <N> Page<N> createPage(List<N> nodes, CursorProvider<N> cursorProvider, boolean hasNextPage, boolean hasPreviousPage) {
        List<Edge<N>> edges = createEdges(nodes, cursorProvider);
        return new GenericPage<>(edges, createPageInfo(edges, hasNextPage, hasPreviousPage));
    }

    public static <N> List<Edge<N>> createEdges(List<N> nodes, CursorProvider<N> cursorProvider) {
        List<Edge<N>> edges = new ArrayList<>(nodes.size());
        int index = 0;
        for (N node : nodes) {
            edges.add(new GenericEdge<>(node, cursorProvider.createCursor(node, index++)));
        }
        return edges;
    }

    public static <N> PageInfo<N> createPageInfo(List<Edge<N>> edges, boolean hasNextPage, boolean hasPreviousPage) {
        return new GenericPageInfo<>(edges.get(0).getCursor(), edges.get(edges.size() - 1).getCursor(), hasNextPage, hasPreviousPage);
    }
}
