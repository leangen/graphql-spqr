package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Optional;

import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;
import io.leangen.graphql.query.ExecutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class OptionalAdapter extends AbstractTypeAdapter<Optional<?>, Object> {

    @Override
    public Object convertOutput(Optional<?> original, AnnotatedType type, InputDeserializer inputDeserializer, ExecutionContext executionContext) {
        return original.map(inner -> executionContext.convertOutput(inner, getSubstituteType(type), inputDeserializer)).orElse(null);
    }

    @Override
    public Optional<?> convertInput(Object substitute, AnnotatedType type, ExecutionContext executionContext) {
        return Optional.ofNullable(executionContext.convertInput(substitute, getSubstituteType(type)));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return ((AnnotatedParameterizedType) original).getAnnotatedActualTypeArguments()[0];
    }
}
