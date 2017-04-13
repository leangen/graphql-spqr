package io.leangen.graphql.execution;

import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface TypeResolver {

    GraphQLObjectType resolveType(TypeRepository typeRepository, TypeMetaDataGenerator typeMetaDataGenerator, Object result);
}
