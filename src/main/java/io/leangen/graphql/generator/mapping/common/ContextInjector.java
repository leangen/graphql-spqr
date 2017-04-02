package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.execution.ResolutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ContextInjector extends InputValueDeserializer {
    
    @Override
    public Object getArgumentValue(Object input, AnnotatedType type, ResolutionContext resolutionContext) {
        return input == null ? resolutionContext.source : super.getArgumentValue(input, type, resolutionContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLContext.class);
    }
}
