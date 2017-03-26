package io.leangen.graphql.execution.relay;

import java.util.List;

/**
 * Created by bojan.tomic on 4/6/16.
 */
public interface Page<N> {
    List<Edge<N>> getEdges();

    PageInfo getPageInfo();
}
