package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLUnionType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedGraphQLUnionType extends GraphQLUnionType implements MappedGraphQLType {

    private final AnnotatedType javaType;

    public MappedGraphQLUnionType(GraphQLUnionType graphQLType, AnnotatedType javaType) {
        super(graphQLType.getName(), graphQLType.getDescription(), graphQLType.getTypes(),
                graphQLType.getTypeResolver());
        this.javaType = javaType;
    }

    @Override
    public AnnotatedType getJavaType() {
        return javaType;
    }
}
