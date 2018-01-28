package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface MappedGraphQLType extends GraphQLType {

    AnnotatedType getJavaType();
}
