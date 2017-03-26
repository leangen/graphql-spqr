package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import io.leangen.graphql.execution.ConnectionRequest;
import io.leangen.graphql.metadata.OperationArgument;

/**
 * Created by bojan.tomic on 7/17/16.
 */
public interface ResolverArgumentBuilder {

    List<OperationArgument> buildResolverArguments(Method resolverMethod, AnnotatedType enclosingType);

    default boolean isExplicitRelayConnectionArgument(Parameter parameter) {
        return ConnectionRequest.class.isAssignableFrom(parameter.getType());
    }
}
