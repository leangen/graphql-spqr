package io.leangen.graphql.execution;

/**
 * Created by bojan.tomic on 3/31/16.
 */
public class ForwardPageRequest {

    private String after;
    private int first;

    ForwardPageRequest(String after, int first) {
        this.after = after;
        this.first = first;
    }

    public String getAfter() {
        return after;
    }

    public int getFirst() {
        return first;
    }
}