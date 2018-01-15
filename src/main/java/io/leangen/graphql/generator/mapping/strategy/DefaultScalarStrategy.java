package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.util.Map;

import io.leangen.geantyref.GenericTypeReflector;

/**
 * The default strategy. Treats raw {@link Object} and the types annotated by
 * {@link io.leangen.graphql.annotations.GraphQLScalar} as scalars.
 */
public class DefaultScalarStrategy extends ObjectScalarStrategy {

    @Override
    public boolean supports(AnnotatedType type) {
        return super.supports(type) || GenericTypeReflector.isSuperType(Map.class, type.getType());
    }
}
