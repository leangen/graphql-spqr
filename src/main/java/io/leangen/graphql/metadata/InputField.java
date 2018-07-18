package io.leangen.graphql.metadata;

import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

public class InputField {

    private final String name;
    private final String description;
    private final AnnotatedType javaType;
    private final AnnotatedType deserializableType;
    private final Object defaultValue;

    public InputField(String name, String description, AnnotatedType javaType, AnnotatedType deserializableType, Object defaultValue) {
        this.name = Utils.requireNonEmpty(name);
        this.description = description;
        this.javaType = Objects.requireNonNull(javaType);
        this.deserializableType = deserializableType != null ? deserializableType : javaType;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    public AnnotatedType getDeserializableType() {
        return deserializableType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
