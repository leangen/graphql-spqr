package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Map;

import io.leangen.graphql.GraphQLRuntime;
import io.leangen.graphql.annotations.GraphQLRootContext;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RootContextInjector implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(Object rawInput, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        String injectionExpression = type.getAnnotation(GraphQLRootContext.class).value();
        Object rootContext = resolutionEnvironment.rootContext instanceof GraphQLRuntime.ContextWrapper
                ? ((GraphQLRuntime.ContextWrapper) resolutionEnvironment.rootContext).getContext()
                : resolutionEnvironment.rootContext;
        return injectionExpression.isEmpty() ? rootContext : extract(rootContext, injectionExpression);
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
