package io.leangen.graphql.generator.types;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;

import graphql.schema.GraphQLUnionType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class MappedGraphQLUnionType extends GraphQLUnionType implements MappedGraphQLType {

    private final AnnotatedType javaType;

    public MappedGraphQLUnionType(GraphQLUnionType graphQLType, AnnotatedType javaType) {
        super(graphQLType.getName(), graphQLType.getDescription(), graphQLType.getTypes(),
                Collections.emptyList(), graphQLType.getTypeResolver());
        this.javaType = javaType;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof GraphQLUnionType &&
                ((GraphQLUnionType) obj).getName().equals(getName()) &&
                ((GraphQLUnionType) obj).getAllTypes().equals(getAllTypes()));
    }
}
