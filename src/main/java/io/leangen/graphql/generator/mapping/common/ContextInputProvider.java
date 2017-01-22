package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.generator.mapping.InputValueProvider;
import io.leangen.graphql.query.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ContextInputProvider implements InputValueProvider {
    
    @Override
    public Object getInputValue(Object substitute, AnnotatedType type, ResolutionContext resolutionContext) {
        return resolutionContext.context;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLContext.class);
    }
}
