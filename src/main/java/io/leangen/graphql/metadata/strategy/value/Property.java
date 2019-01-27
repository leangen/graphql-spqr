package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class Property {

    private final Field field;
    private final Method getter;

    public Property(Field field, Method getter) {
        this.field = Objects.requireNonNull(field);
        this.getter = Objects.requireNonNull(getter);
    }

    public Field getField() {
        return field;
    }

    public Method getGetter() {
        return getter;
    }
}
