package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

import io.leangen.graphql.metadata.OperationArgumentDefaultValue;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class IdentityDefaultValueProvider implements DefaultValueProvider {

    public final OperationArgumentDefaultValue value;

    public IdentityDefaultValueProvider(OperationArgumentDefaultValue value) {
        this.value = value;
    }

    @Override
    public OperationArgumentDefaultValue getDefaultValue(Parameter parameter, AnnotatedType parameterType, OperationArgumentDefaultValue initialValue) {
        return initialValue;
    }
}
