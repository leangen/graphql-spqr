package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLResolverSource;
import io.leangen.graphql.annotations.RelayConnectionRequest;
import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.metadata.QueryArgumentDefaultValue;
import io.leangen.graphql.util.ClassUtils;

public class AnnotatedArgumentExtractor implements QueryResolverArgumentExtractor {

    @Override
    public List<QueryArgument> extractResolverArguments(Method resolverMethod, AnnotatedType enclosingType) {
        List<QueryArgument> queryArguments = new ArrayList<>(resolverMethod.getParameterCount());
        AnnotatedType[] parameterTypes = ClassUtils.getParameterTypes(resolverMethod, enclosingType);
        for (int i = 0; i < resolverMethod.getParameterCount(); i++) {
            Parameter parameter = resolverMethod.getParameters()[i];
            GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
            ClassUtils.checkIfResolvable(parameterTypes[i], resolverMethod); //checks if the type is resolvable
            AnnotatedType parameterType = ClassUtils.stripBounds(parameterTypes[i]);
            try {
                queryArguments.add(new QueryArgument(
                        parameterType,
                        meta != null && !meta.name().isEmpty() ? meta.name() : parameter.getName(),
                        meta != null ? meta.description() : null,
                        defaultValue(parameter, parameterType),
                        parameter.isAnnotationPresent(GraphQLResolverSource.class),
                        parameter.isAnnotationPresent(GraphQLContext.class),
                        parameter.isAnnotationPresent(RelayConnectionRequest.class) || isExplicitRelayConnectionArgument(parameter)
                ));
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(
                        meta.defaultValueProvider().getName() + " must expose a public default constructor", e);
            }
        }
        return queryArguments;
    }

    protected QueryArgumentDefaultValue defaultValue(Parameter parameter, AnnotatedType parameterType) throws IllegalAccessException, InstantiationException {

        GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
        if (meta == null) return QueryArgumentDefaultValue.EMPTY;
        return meta.defaultValueProvider().newInstance().getDefaultValue(parameter, parameterType, defaultValue(meta.defaultValue()));
    }
    
    private QueryArgumentDefaultValue defaultValue(String value) {
        if (GraphQLArgument.NONE.equals(value)) {
            return QueryArgumentDefaultValue.EMPTY;
        } else if (GraphQLArgument.NULL.equals(value)) {
            return QueryArgumentDefaultValue.NULL;
        }
        return new QueryArgumentDefaultValue(value);
    }
}
