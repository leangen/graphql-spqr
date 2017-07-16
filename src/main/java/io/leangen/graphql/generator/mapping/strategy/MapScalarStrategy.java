package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.util.Map;

import io.leangen.geantyref.GenericTypeReflector;

/**
 * A scalar mapping strategy that treats all {@link Map}s as scalars
 */
public class MapScalarStrategy implements ScalarMappingStrategy {
    
    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Map.class, type.getType());
    }
}
