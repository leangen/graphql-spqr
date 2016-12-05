package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

import graphql.schema.GraphQLInterfaceType;
import io.leangen.geantyref.GenericTypeReflector;

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
    public boolean equals(Object that) {
        if (that == this) return true;
        boolean baseEquals = that instanceof GraphQLInterfaceType &&
                ((GraphQLInterfaceType) that).getName().equals(getName()) &&
                ((GraphQLInterfaceType) that).getFieldDefinitions().equals(getFieldDefinitions());

        return baseEquals && (!(that instanceof MappedGraphQLInterfaceType) ||
                GenericTypeReflector.equals(((MappedGraphQLInterfaceType) that).getJavaType(), this.getJavaType()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getFieldDefinitions(), javaType);
    }
}
