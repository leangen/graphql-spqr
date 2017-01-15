package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

import io.leangen.graphql.metadata.QueryArgumentDefaultValue;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class IdentityDefaultValueProvider implements DefaultValueProvider {

    public final QueryArgumentDefaultValue value;

    public IdentityDefaultValueProvider(QueryArgumentDefaultValue value) {
        this.value = value;
    }

    @Override
    public QueryArgumentDefaultValue getDefaultValue(Parameter parameter, AnnotatedType parameterType, QueryArgumentDefaultValue initialValue) {
        return initialValue;
    }
}
