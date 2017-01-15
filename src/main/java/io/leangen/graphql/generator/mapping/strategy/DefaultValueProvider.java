package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

import io.leangen.graphql.metadata.QueryArgumentDefaultValue;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface DefaultValueProvider {
    
    QueryArgumentDefaultValue getDefaultValue(Parameter parameter, AnnotatedType parameterType, QueryArgumentDefaultValue initialValue);
}
