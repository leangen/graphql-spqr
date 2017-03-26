package io.leangen.graphql.execution;

/**
 * Created by bojan.tomic on 3/31/16.
 */
public class BackwardPageRequest {

    private String before;
    private int last;

    BackwardPageRequest(String before, int last) {
        this.before = before;
        this.last = last;
    }

    public String getBefore() {
        return before;
    }

    public int getLast() {
        return last;
    }
}