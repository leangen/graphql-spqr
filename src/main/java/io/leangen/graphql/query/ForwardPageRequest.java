package io.leangen.graphql.query;

/**
 * Created by bojan.tomic on 3/31/16.
 */
public class ForwardPageRequest {

    private String after;
    private int first;

    public ForwardPageRequest(String after, int first) {
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