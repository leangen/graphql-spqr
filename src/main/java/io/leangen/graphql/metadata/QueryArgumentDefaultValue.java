package io.leangen.graphql.metadata;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class QueryArgumentDefaultValue {

    public static final QueryArgumentDefaultValue EMPTY = new QueryArgumentDefaultValue(null, false);
    public static final QueryArgumentDefaultValue NULL = new QueryArgumentDefaultValue(null, true);

    private final Object value;
    private final boolean present;

    public QueryArgumentDefaultValue(Object value) {
        this(value, true);
    }

    private QueryArgumentDefaultValue(Object value, boolean present) {
        this.value = value;
        this.present = present;
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) value;
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isEmpty() {
        return !present || value == null;
    }
}
