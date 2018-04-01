package io.leangen.graphql.extension;

import io.leangen.graphql.GraphQLSchemaGenerator;

import java.util.List;

@FunctionalInterface
public interface ExtensionProvider<T> {
    List<T> getExtensions(GraphQLSchemaGenerator.Configuration config, GraphQLSchemaGenerator.ExtensionList<T> defaults);
}
