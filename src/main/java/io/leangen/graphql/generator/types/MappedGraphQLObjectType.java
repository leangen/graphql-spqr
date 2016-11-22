package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;

import graphql.schema.GraphQLObjectType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedGraphQLObjectType extends GraphQLObjectType implements MappedGraphQLType {

    private final AnnotatedType javaType;

    public MappedGraphQLObjectType(GraphQLObjectType graphQLType, AnnotatedType javaType) {
        super(graphQLType.getName(), graphQLType.getDescription(), graphQLType.getFieldDefinitions(),
                graphQLType.getInterfaces(), Collections.emptyList());
        this.javaType = javaType;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GraphQLObjectType &&
                ((GraphQLObjectType) obj).getName().contentEquals(getName()) &&
                ((GraphQLObjectType) obj).getFieldDefinitions().equals(getFieldDefinitions());
    }
}
