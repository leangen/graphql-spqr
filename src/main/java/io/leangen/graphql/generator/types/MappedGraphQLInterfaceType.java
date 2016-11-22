package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLInterfaceType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedGraphQLInterfaceType extends GraphQLInterfaceType implements MappedGraphQLType {

    private final AnnotatedType javaType;

    public MappedGraphQLInterfaceType(GraphQLInterfaceType graphQLType, AnnotatedType javaType) {
        super(graphQLType.getName(), graphQLType.getDescription(), graphQLType.getFieldDefinitions(),
                graphQLType.getTypeResolver());
        this.javaType = javaType;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof GraphQLInterfaceType &&
                ((GraphQLInterfaceType) obj).getName().contentEquals(getName()) &&
                ((GraphQLInterfaceType) obj).getFieldDefinitions().equals(getFieldDefinitions()));
    }
}
