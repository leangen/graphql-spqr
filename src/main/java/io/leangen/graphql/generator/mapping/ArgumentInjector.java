package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ArgumentInjector {
    
    Object getArgumentValue(Object rawInput, AnnotatedType type, ResolutionContext resolutionContext);

    boolean supports(AnnotatedType type);
}
