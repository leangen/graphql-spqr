package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.mapping.strategy.DefaultValueProvider;

public class QueryArgument {

    private final AnnotatedType javaType;
    private final String name;
    private final String description;
    private final DefaultValueProvider defaultValueProvider;
    private final boolean resolverSource;
    private final boolean context;
    private final boolean relayId;
    private final boolean relayConnection;

    public QueryArgument(AnnotatedType javaType, String name, String description, DefaultValueProvider defaultValueProvider,
                         boolean resolverSource, boolean context, boolean relayConnection) {
        
        Objects.requireNonNull(javaType);
        Objects.requireNonNull(name);
        Objects.requireNonNull(defaultValueProvider);
        
        this.javaType = javaType;
        this.name = name;
        this.description = description;
        this.defaultValueProvider = defaultValueProvider;
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

    public DefaultValueProvider getDefaultValueProvider() {
        return defaultValueProvider;
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
