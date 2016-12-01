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

    public AnnotatedType getJavaType() {
        return javaType;
    }

    @Override
    public boolean equals(Object that) {
        return that == this || (that instanceof GraphQLUnionType &&
                ((GraphQLUnionType) that).getName().equals(this.getName()) &&
                ((GraphQLUnionType) that).getTypes().equals(this.getTypes()));
    }
}
