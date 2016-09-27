package io.leangen.graphql.generator.mapping;

import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.query.conversion.InputConverter;
import io.leangen.graphql.query.conversion.OutputConverter;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public abstract class AbstractTypeAdapter<T, S>
        extends AbstractTypeSubstitutingMapper<S>
        implements InputConverter<T, S>, OutputConverter<T, S> {

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperType(getSourceType().getType(), type.getType());
    }

    private AnnotatedType getSourceType() {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeAdapter.class.getTypeParameters()[0]);
    }
}
