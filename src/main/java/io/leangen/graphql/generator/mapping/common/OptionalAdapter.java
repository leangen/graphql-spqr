package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Optional;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.query.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class OptionalAdapter extends AbstractTypeAdapter<Optional<?>, Object> {

    @Override
    public Object convertOutput(Optional<?> original, AnnotatedType type, ResolutionContext resolutionContext) {
        return original.map(inner -> resolutionContext.convertOutput(inner, getSubstituteType(type))).orElse(null);
    }

    @Override
    public Optional<?> convertInput(Object substitute, AnnotatedType type, ResolutionContext resolutionContext) {
        return Optional.ofNullable(resolutionContext.convertInput(substitute, getSubstituteType(type), resolutionContext));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.getTypeParameter(original, Optional.class.getTypeParameters()[0]);
    }
}
