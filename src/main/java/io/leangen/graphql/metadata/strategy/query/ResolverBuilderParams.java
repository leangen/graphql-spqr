package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public class ResolverBuilderParams {

    private final Supplier<Object> querySourceBeanSupplier;
    private final AnnotatedType beanType;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final String[] basePackages;
    private final GlobalEnvironment environment;

    public ResolverBuilderParams(Supplier<Object> querySourceBeanSupplier, AnnotatedType beanType, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, String[] basePackages, GlobalEnvironment environment) {
        this.querySourceBeanSupplier = querySourceBeanSupplier;
        this.beanType = Objects.requireNonNull(beanType);
        this.inclusionStrategy = Objects.requireNonNull(inclusionStrategy);
        this.typeTransformer = Objects.requireNonNull(typeTransformer);
        this.basePackages = Objects.requireNonNull(basePackages);
        this.environment = Objects.requireNonNull(environment);
    }

    public Supplier<Object> getQuerySourceBeanSupplier() {
        return querySourceBeanSupplier;
    }

    public AnnotatedType getBeanType() {
        return beanType;
    }

    public InclusionStrategy getInclusionStrategy() {
        return inclusionStrategy;
    }

    public TypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    public String[] getBasePackages() {
        return basePackages;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }
}
