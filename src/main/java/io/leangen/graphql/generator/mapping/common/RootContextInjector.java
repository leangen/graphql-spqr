package io.leangen.graphql.generator.mapping.common;

import graphql.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLRootContext;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.ContextUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RootContextInjector implements ArgumentInjector {

    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        String injectionExpression = params.getParameter().getAnnotation(GraphQLRootContext.class).value();
        ResolutionEnvironment env = params.getResolutionEnvironment();
        Object rootContext;
        if (env.batchContext != null) {
            rootContext = ContextUtils.unwrapContext(env.batchContext);
        } else {
            rootContext = ContextUtils.unwrapContext(env.rootContext);
        }
        if (injectionExpression.isEmpty()) {
            if (rootContext == null || params.getType().getType().equals(rootContext.getClass())) {
                return rootContext;
            }
            if (ContextUtils.isDefault(rootContext)) {
                GraphQLContext ctx = (GraphQLContext) rootContext;
                Object result = ctx.get(params.getType().getType());
                if (result != null) {
                    return result;
                }
                result = ctx.get(params.getArgument().getName());
                if (result != null) {
                    return result;
                }
                result = ctx.get(params.getType().getType().getTypeName());
                return result;
            }
        }
        return extract(rootContext, injectionExpression);
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return parameter != null && parameter.isAnnotationPresent(GraphQLRootContext.class);
    }

    private Object extract(Object input, String expression) {
        if (input instanceof GraphQLContext) {
            return ((GraphQLContext) input).get(expression);
        }

        if (input instanceof Map) {
            return ((Map) input).get(expression);
        }

        return ClassUtils.getFieldValue(input, expression);
    }
}
