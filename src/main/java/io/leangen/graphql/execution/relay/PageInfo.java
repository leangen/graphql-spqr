package io.leangen.graphql.execution.relay;

/**
 * Created by bojan.tomic on 5/21/16.
 */
public interface PageInfo<N> {

    String getStartCursor();

    String getEndCursor();

    boolean isHasNextPage();

    boolean isHasPreviousPage();
}
