package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Set;

/**
 * A {@code TypeMapper} is used to map annotated Java types to a GraphQL input or output types, modeled by
 * {@link GraphQLOutputType} and {@link GraphQLInputType} respectively.
 * Method parameter types are mapped to GraphQL input types, while the return types are mapped to GraphQL output types.
 */
public interface TypeMapper {

    GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env);
    GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env);

    boolean supports(AnnotatedElement element, AnnotatedType type);
}
