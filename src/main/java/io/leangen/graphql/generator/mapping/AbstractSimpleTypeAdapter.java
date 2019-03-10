package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

public abstract class AbstractSimpleTypeAdapter<T, S> extends AbstractTypeAdapter<T, S> {

    @SuppressWarnings("WeakerAccess")
    protected final Class<?> rawSourceType;

    protected AbstractSimpleTypeAdapter() {
        this.rawSourceType = ClassUtils.getRawType(sourceType.getType());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperClass(rawSourceType, type);
    }
}
