package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface OutputConverter<T, S> {

    S convertOutput(T original, AnnotatedType type, ResolutionContext resolutionContext);
    
    boolean supports(AnnotatedType type);
}
