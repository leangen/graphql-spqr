package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.value.DefaultValueProvider;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.ReservedStrings;
import io.leangen.graphql.util.Urls;
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

    private static final Logger log = LoggerFactory.getLogger(AnnotatedArgumentBuilder.class);

    @Override
    public List<OperationArgument> buildResolverArguments(ArgumentBuilderParams params) {
        Method resolverMethod = params.getResolverMethod();
        List<OperationArgument> operationArguments = new ArrayList<>(resolverMethod.getParameterCount());
        AnnotatedType[] parameterTypes = ClassUtils.getParameterTypes(resolverMethod, params.getDeclaringType());
        for (int i = 0; i < resolverMethod.getParameterCount(); i++) {
            Parameter parameter = resolverMethod.getParameters()[i];
            if (!params.getInclusionStrategy().includeArgument(parameter, params.getDeclaringType())) {
                continue;
            }
            AnnotatedType parameterType;
            try {
                parameterType = params.getTypeTransformer().transform(parameterTypes[i]);
            } catch (TypeMappingException e) {
                throw TypeMappingException.ambiguousParameterType(resolverMethod, parameter, e);
            }
            operationArguments.add(buildResolverArgument(parameter, parameterType, params));
        }
        return operationArguments;
    }

    protected OperationArgument buildResolverArgument(Parameter parameter, AnnotatedType parameterType, ArgumentBuilderParams builderParams) {
        return new OperationArgument(
                parameterType,
                getArgumentName(parameter, parameterType, builderParams),
                getArgumentDescription(parameter, parameterType, builderParams.getEnvironment().messageBundle),
                defaultValue(parameter, parameterType, builderParams.getEnvironment()),
                parameter,
                parameter.isAnnotationPresent(GraphQLContext.class),
                builderParams.getInclusionStrategy().includeArgumentForMapping(parameter, parameterType, builderParams.getDeclaringType())
        );
    }

    protected String getArgumentName(Parameter parameter, AnnotatedType parameterType, ArgumentBuilderParams builderParams) {
        if (Optional.ofNullable(parameterType.getAnnotation(GraphQLId.class)).filter(GraphQLId::relayId).isPresent()) {
            return GraphQLId.RELAY_ID_FIELD_NAME;
        }
        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        if (meta != null && !meta.name().isEmpty()) {
            return builderParams.getEnvironment().messageBundle.interpolate(meta.name());
        } else {
            if (!parameter.isNamePresent() && builderParams.getInclusionStrategy().includeArgumentForMapping(parameter, parameterType, builderParams.getDeclaringType())) {
                log.warn("No explicit argument name given and the parameter name lost in compilation: "
                        + parameter.getDeclaringExecutable().toGenericString() + "#" + parameter
                        + ". For details and possible solutions see " + Urls.Errors.MISSING_ARGUMENT_NAME);
            }
            return parameter.getName();
        }
    }

    protected String getArgumentDescription(Parameter parameter, AnnotatedType parameterType, MessageBundle messageBundle) {
        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        return meta != null ? messageBundle.interpolate(meta.description()) : null;
    }

    protected DefaultValue defaultValue(Parameter parameter, AnnotatedType parameterType, GlobalEnvironment environment) {

        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        if (meta == null) return DefaultValue.EMPTY;
        try {
            DefaultValue initialValue = ReservedStrings.decodeDefault(environment.messageBundle.interpolate(meta.defaultValue()));
            return defaultValueProvider(meta.defaultValueProvider())
                    .getDefaultValue(parameter, environment.getMappableInputType(parameterType), initialValue);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    meta.defaultValueProvider().getName() + " must expose a public default constructor, or a constructor accepting " + GlobalEnvironment.class.getName(), e);
        }
    }

    protected <T extends DefaultValueProvider> T defaultValueProvider(Class<T> type) throws ReflectiveOperationException {
        try {
            return ClassUtils.instance(type);
        } catch (Exception e) {
            throw new IllegalArgumentException(type.getName() + " must expose a public default constructor", e);
        }
    }
}
