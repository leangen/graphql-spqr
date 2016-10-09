package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public abstract class AbstractTypeAdapter<T, S>
        extends AbstractTypeSubstitutingMapper<S>
        implements InputConverter<T, S>, OutputConverter<T, S> {

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isAssignable(getSourceType().getType(), type.getType());
    }

    private AnnotatedType getSourceType() {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeAdapter.class.getTypeParameters()[0]);
    }
}
