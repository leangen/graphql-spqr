package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.List;

import io.leangen.graphql.metadata.OperationArgument;

/**
 * Created by bojan.tomic on 7/17/16.
 */
public interface ResolverArgumentBuilder {

    List<OperationArgument> buildResolverArguments(Method resolverMethod, AnnotatedType enclosingType);
}
