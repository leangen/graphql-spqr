package io.leangen.graphql.execution.relay.generic;

import io.leangen.graphql.execution.relay.PageInfo;

/**
 * Created by bojan.tomic on 5/21/16.
 */
public class GenericPageInfo<N> implements PageInfo<N> {

    private String startCursor;
    private String endCursor;
    private boolean hasNextPage;
    private boolean hasPreviousPage;

    public GenericPageInfo(String startCursor, String endCursor, boolean hasNextPage, boolean hasPreviousPage) {
        this.startCursor = startCursor;
        this.endCursor = endCursor;
        this.hasNextPage = hasNextPage;
        this.hasPreviousPage = hasPreviousPage;
    }

    public String getStartCursor() {
        return startCursor;
    }

    public String getEndCursor() {
        return endCursor;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public boolean isHasPreviousPage() {
        return hasPreviousPage;
    }
}
