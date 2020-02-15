package io.leangen.graphql.generator;

import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.graphql.util.GraphQLUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeCache {

    private final Map<String, GraphQLNamedType> knownTypes;


    TypeCache(Collection<GraphQLNamedType> knownTypes) {
        this.knownTypes = knownTypes.stream().collect(Collectors.toMap(GraphQLNamedType::getName, Function.identity()));
    }

    public void register(String typeName) {
        knownTypes.put(typeName, null);
    }

    public boolean contains(String typeName) {
        return knownTypes.containsKey(typeName);
    }

    GraphQLType resolveType(String typeName) {
        GraphQLType resolved = knownTypes.get(typeName);
        if (resolved instanceof GraphQLTypeReference) {
            throw new IllegalStateException("Type " + typeName + " is not yet resolvable");
        }
        return resolved;
    }

    void completeType(GraphQLOutputType type) {
        GraphQLNamedType namedType = GraphQLUtils.unwrap(type);
        if (!(namedType instanceof GraphQLTypeReference)) {
            knownTypes.put(namedType.getName(), namedType);
        }
    }

    void resolveTypeReferences(TypeRegistry typeRegistry) {
        typeRegistry.resolveTypeReferences(knownTypes);
    }
}
