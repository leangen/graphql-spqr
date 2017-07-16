package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.GraphQLScalar;

/**
 * A scalar mapping strategy that treats only the types explicitly annotated with
 * {@link GraphQLScalar} as scalars
 */
public class AnnotatedScalarStrategy implements ScalarMappingStrategy {
    
    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class);
    }
}
