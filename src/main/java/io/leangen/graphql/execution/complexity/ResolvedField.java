package io.leangen.graphql.execution.complexity;

import graphql.schema.DataFetchingFieldSelectionSet;
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
    private final DataFetchingFieldSelectionSet selectionSet;

    ResolvedField(FieldCoordinates coordinates, Resolver resolver, GraphQLOutputType type,
                  Map<String, Object> arguments, DataFetchingFieldSelectionSet selectionSet) {
        this.coordinates = coordinates;
        this.fieldType = (GraphQLNamedOutputType) GraphQLUtils.unwrap(type);
        this.resolver = resolver;
        this.arguments = arguments;
        this.selectionSet = selectionSet;
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

    DataFetchingFieldSelectionSet getSelectionSet() {
        return selectionSet;
    }

    Resolver getResolver() {
        return resolver;
    }

    @Override
    public String toString() {
        return coordinates.toString();
    }
}
