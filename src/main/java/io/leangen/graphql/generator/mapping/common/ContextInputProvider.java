package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.GraphQLRootContext;
import io.leangen.graphql.execution.ResolutionContext;
import io.leangen.graphql.generator.mapping.ArgumentInjector;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ContextInputProvider implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(Object substitute, AnnotatedType type, ResolutionContext resolutionContext) {
        return resolutionContext.context;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLRootContext.class);
    }
}
