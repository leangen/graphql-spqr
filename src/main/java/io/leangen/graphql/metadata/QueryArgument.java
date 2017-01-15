package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

import io.leangen.graphql.annotations.RelayId;

public class QueryArgument {

    private final AnnotatedType javaType;
    private final String name;
    private final String description;
    private final QueryArgumentDefaultValue defaultValue;
    private final boolean resolverSource;
    private final boolean context;
    private final boolean relayId;
    private final boolean relayConnection;

    public QueryArgument(AnnotatedType javaType, String name, String description, QueryArgumentDefaultValue defaultValue,
                         boolean resolverSource, boolean context, boolean relayConnection) {
        
        Objects.requireNonNull(javaType);
        Objects.requireNonNull(name);
        Objects.requireNonNull(defaultValue);
        
        this.javaType = javaType;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.resolverSource = resolverSource;
        this.context = context;
        this.relayId = javaType.isAnnotationPresent(RelayId.class);
        this.relayConnection = relayConnection;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public QueryArgumentDefaultValue getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isResolverSource() {
        return resolverSource;
    }

    public boolean isRelayId() {
        return relayId;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    public boolean isContext() {
        return context;
    }

    public boolean isRelayConnection() {
        return relayConnection;
    }

}
