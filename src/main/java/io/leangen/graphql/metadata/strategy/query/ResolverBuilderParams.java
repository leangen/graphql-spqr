package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class ResolverBuilderParams {

    private final Object querySourceBean;
    private final AnnotatedType beanType;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final String[] basePackages;
    private final GlobalEnvironment environment;

    ResolverBuilderParams(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, String[] basePackages, GlobalEnvironment environment) {
        this.querySourceBean = querySourceBean;
        this.beanType = Objects.requireNonNull(beanType);
        this.inclusionStrategy = Objects.requireNonNull(inclusionStrategy);
        this.typeTransformer = Objects.requireNonNull(typeTransformer);
        this.basePackages = Objects.requireNonNull(basePackages);
        this.environment = Objects.requireNonNull(environment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Object getQuerySourceBean() {
        return querySourceBean;
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

    public static class Builder {

        private Object querySourceBean;
        private AnnotatedType beanType;
        private InclusionStrategy inclusionStrategy;
        private TypeTransformer typeTransformer;
        private String[] basePackages;
        private GlobalEnvironment environment;

        public Builder withQuerySourceBean(Object querySourceBean) {
            this.querySourceBean = querySourceBean;
            return this;
        }

        public Builder withBeanType(AnnotatedType beanType) {
            this.beanType = beanType;
            return this;
        }

        public Builder withInclusionStrategy(InclusionStrategy inclusionStrategy) {
            this.inclusionStrategy = inclusionStrategy;
            return this;
        }

        public Builder withTypeTransformer(TypeTransformer typeTransformer) {
            this.typeTransformer = typeTransformer;
            return this;
        }

        public Builder withBasePackages(String[] basePackages) {
            this.basePackages = basePackages;
            return this;
        }

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public ResolverBuilderParams build() {
            return new ResolverBuilderParams(querySourceBean, beanType, inclusionStrategy, typeTransformer, basePackages, environment);
        }
    }
}
