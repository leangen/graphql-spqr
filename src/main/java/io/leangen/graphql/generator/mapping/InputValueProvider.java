package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.query.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface InputValueProvider {
    
    Object getInputValue(Object rawInput, AnnotatedType type, ResolutionContext resolutionContext);

    boolean supports(AnnotatedType type);
}
