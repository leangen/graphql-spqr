package io.leangen.graphql.metadata;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class OperationArgumentDefaultValue {

    public static final OperationArgumentDefaultValue EMPTY = new OperationArgumentDefaultValue(null, false);
    public static final OperationArgumentDefaultValue NULL = new OperationArgumentDefaultValue(null, true);

    private final Object value;
    private final boolean present;

    public OperationArgumentDefaultValue(Object value) {
        this(value, true);
    }

    private OperationArgumentDefaultValue(Object value, boolean present) {
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
