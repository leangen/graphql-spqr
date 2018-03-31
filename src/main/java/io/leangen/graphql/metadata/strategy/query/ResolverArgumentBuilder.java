package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by bojan.tomic on 7/17/16.
 */
public interface ResolverArgumentBuilder {

    List<OperationArgument> buildResolverArguments(Method resolverMethod, AnnotatedType enclosingType, InclusionStrategy inclusionStrategy);
}
