package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;

/**
 * Treats raw {@link Object} and the types annotated by
 * {@link io.leangen.graphql.annotations.GraphQLScalar} as scalars.
 */
public class ObjectScalarStrategy extends AnnotatedScalarStrategy {

    @Override
    public boolean supports(AnnotatedType type) {
        return super.supports(type) || Object.class.equals(type.getType());
    }
}
