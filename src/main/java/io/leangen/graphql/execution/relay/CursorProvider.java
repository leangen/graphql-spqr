package io.leangen.graphql.execution.relay;

/**
 * Created by bojan.tomic on 5/17/16.
 */
@FunctionalInterface
public interface CursorProvider<N> {

    String createCursor(N node, int index);
}
