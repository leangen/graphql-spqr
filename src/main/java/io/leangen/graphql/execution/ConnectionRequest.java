package io.leangen.graphql.execution;

import java.util.List;
import java.util.Map;

/**
 * Created by bojan.tomic on 3/31/16.
 */
public class ConnectionRequest {

    private static final String AFTER = "after";
    private static final String FIRST = "first";
    private static final String BEFORE = "before";
    private static final String LAST = "last";
    private static final String ORDER_BY = "orderBy";

    private List<SortField> sortFields;
    private ForwardPageRequest forwardPageRequest;
    private BackwardPageRequest backwardPageRequest;

    ConnectionRequest(Map<String, Object> arguments) {
        String after = (String) arguments.get(AFTER);
        int first = Integer.parseInt(arguments.getOrDefault(FIRST, "0").toString());
        String before = (String) arguments.get(BEFORE);
        int last = Integer.parseInt(arguments.getOrDefault(LAST, "0").toString());

        if (after != null || first != 0) {
            this.forwardPageRequest = new ForwardPageRequest(after, first);
        }
        if (before != null || last != 0) {
            this.backwardPageRequest = new BackwardPageRequest(before, last);
        }
    }

    static boolean isConnectionArgumentName(String argumentName) {
        return AFTER.equals(argumentName) || FIRST.equals(argumentName) ||
                BEFORE.equals(argumentName) || LAST.equals(argumentName) ||
                ORDER_BY.equals(argumentName);
    }

    public List<SortField> getSortFields() {
        return sortFields;
    }

    public ForwardPageRequest getForwardPageRequest() {
        return forwardPageRequest;
    }

    public BackwardPageRequest getBackwardPageRequest() {
        return backwardPageRequest;
    }
    
    public Object getParameter(String name) {
        switch (name) {
            case FIRST: return getForwardPageRequest().getFirst();
            case AFTER: return getForwardPageRequest().getAfter();
            case LAST: return getBackwardPageRequest().getLast();
            case BEFORE: return getBackwardPageRequest().getBefore();
            case ORDER_BY: return this.sortFields;
            default: throw new IllegalArgumentException("Parameter " + name + " is not a valid connection request parameter");
        }
    }
}
