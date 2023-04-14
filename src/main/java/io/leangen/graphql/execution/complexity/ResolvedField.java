package io.leangen.graphql.execution.complexity;

import graphql.normalized.ExecutableNormalizedField;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.GraphQLUtils;

import java.util.Map;

class ResolvedField {

    private final FieldCoordinates coordinates;
    private final GraphQLNamedOutputType fieldType;
    private final Map<String, Object> arguments;
    private final Resolver resolver;
    private final ExecutableNormalizedField field;

    ResolvedField(FieldCoordinates coordinates, Resolver resolver, GraphQLOutputType type,
                  Map<String, Object> arguments, ExecutableNormalizedField field) {
        this.coordinates = coordinates;
        this.fieldType = (GraphQLNamedOutputType) GraphQLUtils.unwrap(type);
        this.resolver = resolver;
        this.arguments = arguments;
        this.field = field;
    }

    FieldCoordinates getCoordinates() {
        return coordinates;
    }

    GraphQLNamedOutputType getFieldType() {
        return fieldType;
    }

    Map<String, Object> getArguments() {
        return arguments;
    }

    ExecutableNormalizedField getField() {
        return field;
    }

    Resolver getResolver() {
        return resolver;
    }

    @Override
    public String toString() {
        return coordinates.toString();
    }
}
