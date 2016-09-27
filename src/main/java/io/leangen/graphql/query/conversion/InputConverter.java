package io.leangen.graphql.query.conversion;

import java.lang.reflect.AnnotatedType;

/**
 * @param <T> The argument type of an exposed method or field
 * @param <S> The substitute type as which the argument values are to be deserialized
 */
public interface InputConverter<T, S> {

    T convertInput(S substitute);
    boolean supports(AnnotatedType type);

    /**
     * The returned type has to erase to {@code Class<S>}
     * @param original
     * @return
     */
    AnnotatedType getSubstituteType(AnnotatedType original);
}
