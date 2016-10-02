package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface OutputConverter<T, S> {

    S convertOutput(T original);
    boolean supports(AnnotatedType type);
}
