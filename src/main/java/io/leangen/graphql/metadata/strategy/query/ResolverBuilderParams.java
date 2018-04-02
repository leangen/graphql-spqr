package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;

public class ResolverBuilderParams {

    private final Object querySourceBean;
    private final AnnotatedType beanType;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final String[] basePackages;

    public ResolverBuilderParams(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, String[] basePackages) {
        this.querySourceBean = querySourceBean;
        this.beanType = beanType;
        this.inclusionStrategy = inclusionStrategy;
        this.typeTransformer = typeTransformer;
        this.basePackages = basePackages;
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
}
