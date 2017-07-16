package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Optional;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class OptionalAdapter extends AbstractTypeAdapter<Optional<?>, Object> {

    @Override
    public Object convertOutput(Optional<?> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.map(inner -> resolutionEnvironment.convertOutput(inner, getSubstituteType(type))).orElse(null);
    }

    @Override
    public Optional<?> convertInput(Object substitute, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return Optional.ofNullable(resolutionEnvironment.convertInput(substitute, getSubstituteType(type)));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType innerType = GenericTypeReflector.getTypeParameter(original, Optional.class.getTypeParameters()[0]);
        return ClassUtils.addAnnotations(innerType, original.getAnnotations());
    }
}
