package io.leangen.graphql.extension;

import io.leangen.graphql.GraphQLSchemaGenerator;

import java.util.List;

@FunctionalInterface
public interface ExtensionProvider<C, D> {
    List<D> getExtensions(C config, GraphQLSchemaGenerator.ExtensionList<D> defaults);
}
