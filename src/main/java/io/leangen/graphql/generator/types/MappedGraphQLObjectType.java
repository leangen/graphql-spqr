package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLObjectType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedGraphQLObjectType extends GraphQLObjectType implements MappedGraphQLType {

    private final AnnotatedType javaType;

    public MappedGraphQLObjectType(GraphQLObjectType graphQLType, AnnotatedType javaType) {
        super(graphQLType.getName(), graphQLType.getDescription(), graphQLType.getFieldDefinitions(),
                graphQLType.getInterfaces());
        this.javaType = javaType;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    @Override
    public boolean equals(Object that) {
        return this == that || (that instanceof GraphQLObjectType &&
                ((GraphQLObjectType) that).getName().equals(this.getName()) &&
                ((GraphQLObjectType) that).getFieldDefinitions().equals(this.getFieldDefinitions()) &&
                ((GraphQLObjectType) that).getInterfaces().equals(this.getInterfaces()));
    }
}
