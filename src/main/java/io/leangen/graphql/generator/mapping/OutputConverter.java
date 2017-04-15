package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.ResolutionEnvironment;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface OutputConverter<T, S> {

    S convertOutput(T original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment);
    
    boolean supports(AnnotatedType type);
}
