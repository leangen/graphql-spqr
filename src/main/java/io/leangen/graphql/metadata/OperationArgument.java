package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

import io.leangen.graphql.annotations.RelayId;

public class OperationArgument {

    private final AnnotatedType javaType;
    private final String name;
    private final String description;
    private final OperationArgumentDefaultValue defaultValue;
    private final boolean context;
    private final boolean relayId;
    private final boolean mappable;

    public OperationArgument(AnnotatedType javaType, String name, String description, OperationArgumentDefaultValue defaultValue,
                             boolean context, boolean mappable) {
        
        Objects.requireNonNull(javaType);
        Objects.requireNonNull(name);
        Objects.requireNonNull(defaultValue);
        
        this.javaType = javaType;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.context = context;
        this.relayId = javaType.isAnnotationPresent(RelayId.class);
        this.mappable = mappable;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public OperationArgumentDefaultValue getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isContext() {
        return context;
    }

    public boolean isRelayId() {
        return relayId;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    public boolean isMappable() {
        return mappable;
    }
}
