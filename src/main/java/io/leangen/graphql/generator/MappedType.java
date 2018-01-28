package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedType {
    public final AnnotatedType javaType;
    public final GraphQLOutputType graphQLType;

    MappedType(AnnotatedType javaType, GraphQLOutputType graphQLType) {
        this.javaType = javaType;
        this.graphQLType = graphQLType;
    }
    
    public GraphQLObjectType getAsObjectType() {
        return (GraphQLObjectType) graphQLType;
    }
}
