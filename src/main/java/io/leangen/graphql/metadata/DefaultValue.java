package io.leangen.graphql.metadata;

import java.util.Objects;
import java.util.function.Function;

public class DefaultValue {

    private final Object value;
    private final boolean set;

    public static final DefaultValue EMPTY = new DefaultValue(null, false);
    public static final DefaultValue NULL = new DefaultValue(null, true);

    private DefaultValue(Object value, boolean set) {
        this.value = value;
        this.set = set;
    }

    public DefaultValue(Object value) {
        this(value, true);
    }

    public static DefaultValue ofNullable(Object value) {
        return new DefaultValue(value, value != null);
    }

    public DefaultValue map(Function<Object, Object> mapper) {
        if (!isSet() || value == null) return this;
        return new DefaultValue(mapper.apply(value));
    }

    public Object getValue() {
        return value;
    }

    public boolean isSet() {
        return set;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultValue)) return false;
        DefaultValue that = (DefaultValue) o;
        return set == that.set && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, set);
    }
}
