package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;

public interface ScalarMappingStrategy {

    boolean supports(AnnotatedType type);
}
