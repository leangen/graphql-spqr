package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLObjectType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedType {
    public AnnotatedType javaType;
    public GraphQLObjectType graphQLType;

    public MappedType(AnnotatedType javaType, GraphQLObjectType graphQLType) {
        this.javaType = javaType;
        this.graphQLType = graphQLType;
    }
}
