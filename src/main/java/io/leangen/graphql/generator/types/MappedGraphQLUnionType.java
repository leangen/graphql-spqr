package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

import graphql.schema.GraphQLUnionType;
import io.leangen.geantyref.GenericTypeReflector;

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
        if (that == this) return true;
        boolean baseEquals = that instanceof GraphQLUnionType &&
                ((GraphQLUnionType) that).getName().equals(this.getName()) &&
                ((GraphQLUnionType) that).getTypes().equals(this.getTypes());
        return baseEquals && (!(that instanceof MappedGraphQLUnionType) ||
                GenericTypeReflector.equals(((MappedGraphQLUnionType) that).getJavaType(), this.getJavaType()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getTypes(), javaType);
    }
}
