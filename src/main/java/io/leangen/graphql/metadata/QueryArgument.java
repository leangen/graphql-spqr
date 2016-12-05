package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.RelayId;

public class QueryArgument {

    private final AnnotatedType javaType;
    private final String name;
    private final String description;
    private final DefaultValue defaultValue;
    private final boolean resolverSource;
    private final boolean context;
    private final boolean relayId;
    private final boolean relayConnection;

    public QueryArgument(AnnotatedType javaType, String name, String description, DefaultValue defaultValue,
                         boolean resolverSource, boolean context, boolean relayConnection) {
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

    public DefaultValue getDefaultValue() {
        return defaultValue;
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

    public static class DefaultValue {

        private static final DefaultValue EMPTY = new DefaultValue(null, false);
        private static final DefaultValue NULL = new DefaultValue(null, true);

        private final String value;
        private final boolean present;

        private DefaultValue(String value, boolean present) {
            this.value = value;
            this.present = present;
        }

        public static DefaultValue of(String value) {
            if (GraphQLArgument.NONE.equals(value)) {
                return EMPTY;
            } else if (GraphQLArgument.NULL.equals(value)) {
                return NULL;
            }
            return new DefaultValue(value, true);
        }

        public static DefaultValue empty() {
            return EMPTY;
        }

        public String get() {
            return value;
        }

        public boolean isPresent() {
            return present;
        }
    }
}
