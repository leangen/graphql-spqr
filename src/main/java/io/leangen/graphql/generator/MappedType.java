package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLOutputType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedType {
    public AnnotatedType javaType;
    public GraphQLOutputType graphQLType;

    public MappedType(AnnotatedType javaType, GraphQLOutputType graphQLType) {
        this.javaType = javaType;
        this.graphQLType = graphQLType;
    }
}
