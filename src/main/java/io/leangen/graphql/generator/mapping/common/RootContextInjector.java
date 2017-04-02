package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Map;

import io.leangen.graphql.annotations.GraphQLRootContext;
import io.leangen.graphql.execution.ResolutionContext;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RootContextInjector implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(Object rawInput, AnnotatedType type, ResolutionContext resolutionContext) {
        String injectionExpression = type.getAnnotation(GraphQLRootContext.class).value();
        return injectionExpression.isEmpty() ? resolutionContext.context : extract(resolutionContext.context, injectionExpression);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLRootContext.class);
    }
    
    private Object extract(Object input, String expression) {
        if (input instanceof Map) {
            return ((Map) input).get(expression);
        }
        return ClassUtils.getFieldValue(input, expression);
    }
}
