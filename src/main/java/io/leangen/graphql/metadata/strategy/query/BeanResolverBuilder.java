package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * A resolver builder that exposes all public getter methods
 */
public class BeanResolverBuilder extends PublicResolverBuilder {

    public BeanResolverBuilder(String... basePackages) {
        super(basePackages);
        this.operationInfoGenerator = new PropertyOperationInfoGenerator();
    }

    @Override
    protected boolean isQuery(Method method, ResolverBuilderParams params) {
        return super.isQuery(method, params) && ClassUtils.isGetter(method);
    }

    @Override
    protected boolean isMutation(Method method, ResolverBuilderParams params) {
        return super.isMutation(method, params) && ClassUtils.isSetter(method);
    }
}
