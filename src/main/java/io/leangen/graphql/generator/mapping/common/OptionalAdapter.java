package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Optional;

import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class OptionalAdapter<T> extends AbstractTypeAdapter<Optional<T>, T> {

    @Override
    public T convertOutput(Optional<T> original) {
        return original.orElse(null);
    }

    @Override
    public Optional<T> convertInput(T substitute) {
        return Optional.ofNullable(substitute);
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return ((AnnotatedParameterizedType) original).getAnnotatedActualTypeArguments()[0];
    }
}
