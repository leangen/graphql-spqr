package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.ResolutionEnvironment;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ArgumentInjector {
    
    Object getArgumentValue(Object rawInput, AnnotatedType type, ResolutionEnvironment resolutionEnvironment);

    boolean supports(AnnotatedType type);
}
