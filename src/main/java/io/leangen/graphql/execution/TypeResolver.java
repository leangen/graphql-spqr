package io.leangen.graphql.execution;

import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface TypeResolver {

    GraphQLObjectType resolveType(TypeRepository typeRepository, TypeInfoGenerator typeInfoGenerator, Object result);
}
