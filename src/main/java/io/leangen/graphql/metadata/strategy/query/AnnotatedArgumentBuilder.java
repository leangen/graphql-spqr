package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.OperationArgumentDefaultValue;
import io.leangen.graphql.util.ClassUtils;

public class AnnotatedArgumentBuilder implements ResolverArgumentBuilder {

    @Override
    public List<OperationArgument> buildResolverArguments(Method resolverMethod, AnnotatedType enclosingType) {
        List<OperationArgument> operationArguments = new ArrayList<>(resolverMethod.getParameterCount());
        AnnotatedType[] parameterTypes = ClassUtils.getParameterTypes(resolverMethod, enclosingType);
        for (int i = 0; i < resolverMethod.getParameterCount(); i++) {
            Parameter parameter = resolverMethod.getParameters()[i];
            GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
            ClassUtils.checkIfResolvable(parameterTypes[i], resolverMethod); //checks if the type is resolvable
            AnnotatedType parameterType = ClassUtils.stripBounds(parameterTypes[i]);
            parameterType = ClassUtils.addAnnotations(parameterType, parameter.getAnnotations());
            try {
                operationArguments.add(new OperationArgument(
                        parameterType,
                        meta != null && !meta.name().isEmpty() ? meta.name() : parameter.getName(),
                        meta != null ? meta.description() : null,
                        defaultValue(parameter, parameterType),
                        parameter.isAnnotationPresent(GraphQLContext.class),
                        isMappable(parameter)
                ));
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(
                        meta.defaultValueProvider().getName() + " must expose a public default constructor", e);
            }
        }
        return operationArguments;
    }

    protected OperationArgumentDefaultValue defaultValue(Parameter parameter, AnnotatedType parameterType) throws IllegalAccessException, InstantiationException {

        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        if (meta == null) return OperationArgumentDefaultValue.EMPTY;
        return meta.defaultValueProvider().newInstance().getDefaultValue(parameter, parameterType, defaultValue(meta.defaultValue()));
    }
    
    private OperationArgumentDefaultValue defaultValue(String value) {
        if (GraphQLArgument.NONE.equals(value)) {
            return OperationArgumentDefaultValue.EMPTY;
        } else if (GraphQLArgument.NULL.equals(value)) {
            return OperationArgumentDefaultValue.NULL;
        }
        return new OperationArgumentDefaultValue(value);
    }
    
    private boolean isMappable(Parameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
                .noneMatch(ann -> ann.annotationType().isAnnotationPresent(GraphQLIgnore.class));
    }
}
