package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.execution.ResolutionEnvironment;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ContextInjector extends InputValueDeserializer {
    
    @Override
    public Object getArgumentValue(Object input, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return input == null ? resolutionEnvironment.context : super.getArgumentValue(input, type, resolutionEnvironment);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLContext.class);
    }
}
