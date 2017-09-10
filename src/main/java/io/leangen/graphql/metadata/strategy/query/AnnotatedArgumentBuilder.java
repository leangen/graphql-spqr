package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.OperationArgumentDefaultValue;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.util.ClassUtils;

public class AnnotatedArgumentBuilder implements ResolverArgumentBuilder {

    private final TypeTransformer transformer;

    public AnnotatedArgumentBuilder(TypeTransformer transformer) {
        this.transformer = transformer;
    }
    
    @Override
    public List<OperationArgument> buildResolverArguments(Method resolverMethod, AnnotatedType declaringType) {
        List<OperationArgument> operationArguments = new ArrayList<>(resolverMethod.getParameterCount());
        AnnotatedType[] parameterTypes = ClassUtils.getParameterTypes(resolverMethod, declaringType);
        for (int i = 0; i < resolverMethod.getParameterCount(); i++) {
            Parameter parameter = resolverMethod.getParameters()[i];
            if (parameter.isSynthetic() || parameter.isImplicit()) continue;
            AnnotatedType parameterType;
            try {
                parameterType = transformer.transform(parameterTypes[i]);
            } catch (TypeMappingException e) {
                throw new TypeMappingException(resolverMethod, parameter, e);
            }
            parameterType = ClassUtils.addAnnotations(parameterType, parameter.getAnnotations());
            operationArguments.add(new OperationArgument(
                    parameterType,
                    getArgumentName(parameter, parameterType),
                    getArgumentDescription(parameter, parameterType),
                    defaultValue(parameter, parameterType),
                    parameter.isAnnotationPresent(GraphQLContext.class),
                    isMappable(parameter)
            ));
        }
        return operationArguments;
    }

    protected String getArgumentName(Parameter parameter, AnnotatedType parameterType) {
        if (Optional.ofNullable(parameterType.getAnnotation(GraphQLId.class)).filter(GraphQLId::relayId).isPresent()) {
            return GraphQLId.RELAY_ID_FIELD_NAME;
        }
        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        return meta != null && !meta.name().isEmpty() ? meta.name() : parameter.getName();
    }

    protected String getArgumentDescription(Parameter parameter, AnnotatedType parameterType) {
        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        return meta != null ? meta.description() : null;
    }

    protected OperationArgumentDefaultValue defaultValue(Parameter parameter, AnnotatedType parameterType) {

        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        if (meta == null) return OperationArgumentDefaultValue.EMPTY;
        try {
            return meta.defaultValueProvider().newInstance().getDefaultValue(parameter, parameterType, defaultValue(meta.defaultValue()));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    meta.defaultValueProvider().getName() + " must expose a public default constructor", e);
        }
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
        return !parameter.isAnnotationPresent(GraphQLIgnore.class) && Arrays.stream(parameter.getAnnotations())
                .noneMatch(ann -> ann.annotationType().isAnnotationPresent(GraphQLIgnore.class));
    }
}
