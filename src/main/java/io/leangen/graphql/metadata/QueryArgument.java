package io.leangen.graphql.metadata;

import io.leangen.graphql.annotations.RelayId;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 3/4/16.
 */
public class QueryArgument {

    private AnnotatedType javaType;
    private String name;
    private String description;
    private boolean required;
    private boolean resolverSource;
    private boolean context;
    private boolean relayId;
    private boolean relayConnection;

    public QueryArgument(AnnotatedType javaType, String name, String description, boolean required, boolean resolverSource,
                         boolean context, boolean relayConnection) {
        this.javaType = javaType;
        this.name = name;
        this.description = description;
        this.required = required;
        this.resolverSource = resolverSource;
        this.context = context;
        this.relayId = javaType.isAnnotationPresent(RelayId.class);
        this.relayConnection = relayConnection;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isResolverSource() {
        return resolverSource;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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
