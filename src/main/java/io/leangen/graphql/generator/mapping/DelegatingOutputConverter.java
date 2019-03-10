package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.List;

public interface DelegatingOutputConverter<T, S> extends OutputConverter<T, S> {

    List<AnnotatedType> getDerivedTypes(AnnotatedType type);

    /**
     * A {@code DelegatingOutputConverter} is considered <i>transparent</i> if its sole purpose is to delegate to other
     * converters, and contains no other logic of its own. In other words, if there is no reason to invoke it without
     * other applicable converters present.
     * This flag is purely a performance optimization hint, as is always safe to leave on {@code false}.
     *
     * @return A boolean signifying whether the converter is <i>transparent</i>.
     */
    default boolean isTransparent() {
        return false;
    }
}
