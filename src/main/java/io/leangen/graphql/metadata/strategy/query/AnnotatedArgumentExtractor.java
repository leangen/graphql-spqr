package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLResolverSource;
import io.leangen.graphql.annotations.RelayConnectionRequest;
import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bojan.tomic on 7/17/16.
 */
public class AnnotatedArgumentExtractor implements QueryResolverArgumentExtractor {

    @Override
    public List<QueryArgument> extractResolverArguments(Method resolverMethod, AnnotatedType enclosingType) {
        List<QueryArgument> queryArguments = new ArrayList<>(resolverMethod.getParameterCount());
        AnnotatedType[] parameterTypes = ClassUtils.getParameterTypes(resolverMethod, enclosingType);
        for (int i = 0; i < resolverMethod.getParameterCount(); i++) {
            Parameter parameter = resolverMethod.getParameters()[i];
            GraphQLArgument meta = parameter.getAnnotation(GraphQLArgument.class);
            ClassUtils.checkIfResolvable(parameterTypes[i], resolverMethod); //checks if the type is resolvable
            queryArguments.add(new QueryArgument(
                    parameterTypes[i],
                    meta != null && !meta.name().isEmpty() ? meta.name() : parameter.getName(),
                    meta != null ? meta.description() : null,
                    meta != null && meta.required(),
                    parameter.isAnnotationPresent(GraphQLResolverSource.class),
                    parameter.isAnnotationPresent(GraphQLContext.class),
                    parameter.isAnnotationPresent(RelayConnectionRequest.class) || isExplicitRelayConnectionArgument(parameter)
            ));
        }
        return queryArguments;
    }
}
