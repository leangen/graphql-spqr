package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.annotations.GraphQLRootContext;
import io.leangen.graphql.execution.ContextWrapper;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RootContextInjector implements ArgumentInjector {
    
    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        String injectionExpression = params.getType().getAnnotation(GraphQLRootContext.class).value();
        ResolutionEnvironment env = params.getResolutionEnvironment();
        Object rootContext = env.rootContext instanceof ContextWrapper
                ? ((ContextWrapper) env.rootContext).getContext()
                : env.rootContext;
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
