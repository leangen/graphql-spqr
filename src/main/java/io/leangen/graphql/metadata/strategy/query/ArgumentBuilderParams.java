package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;

@SuppressWarnings("WeakerAccess")
public class ArgumentBuilderParams {

    private final Method resolverMethod;
    private final AnnotatedType declaringType;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final GlobalEnvironment environment;

    ArgumentBuilderParams(Method resolverMethod, AnnotatedType declaringType, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, GlobalEnvironment environment) {
        this.resolverMethod = resolverMethod;
        this.declaringType = declaringType;
        this.inclusionStrategy = inclusionStrategy;
        this.typeTransformer = typeTransformer;
        this.environment = environment;
    }

    public Method getResolverMethod() {
        return resolverMethod;
    }

    public AnnotatedType getDeclaringType() {
        return declaringType;
    }

    public InclusionStrategy getInclusionStrategy() {
        return inclusionStrategy;
    }

    public TypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }
}
