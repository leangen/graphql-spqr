package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.OperationArgumentDefaultValue;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Urls;
import io.leangen.graphql.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class AnnotatedArgumentBuilder implements ResolverArgumentBuilder {

    private final TypeTransformer transformer;

    private static final Logger log = LoggerFactory.getLogger(AnnotatedArgumentBuilder.class);

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
                    !ClassUtils.hasAnnotation(parameter, GraphQLIgnore.class)
            ));
        }
        return operationArguments;
    }

    protected String getArgumentName(Parameter parameter, AnnotatedType parameterType) {
        if (Optional.ofNullable(parameterType.getAnnotation(GraphQLId.class)).filter(GraphQLId::relayId).isPresent()) {
            return GraphQLId.RELAY_ID_FIELD_NAME;
        }
        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        if (meta != null && !meta.name().isEmpty()) {
            return meta.name();
        } else {
            if (!parameter.isNamePresent() && !ClassUtils.hasAnnotation(parameter, GraphQLIgnore.class)) {
                log.warn("No explicit argument name given and the parameter name lost in compilation: "
                        + parameter.getDeclaringExecutable().toGenericString() + "#" + parameter.toString()
                        + ". For details and possible solutions see " + Urls.Errors.MISSING_ARGUMENT_NAME);
            }
            return parameter.getName();
        }
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
        } else if (Utils.NULL.equals(value)) {
            return OperationArgumentDefaultValue.NULL;
        }
        return new OperationArgumentDefaultValue(value);
    }
}
