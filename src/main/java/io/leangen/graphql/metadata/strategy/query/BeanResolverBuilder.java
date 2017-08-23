package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.Method;

import io.leangen.graphql.util.ClassUtils;

/**
 * A resolver builder that exposes all public getter methods
 */
public class BeanResolverBuilder extends PublicResolverBuilder {

    public BeanResolverBuilder(String basePackage) {
        super(basePackage);
        this.operationNameGenerator = new BeanOperationNameGenerator();
    }

    @Override
    protected boolean isQuery(Method method) {
        return super.isQuery(method) && ClassUtils.isGetter(method);
    }

    @Override
    protected boolean isMutation(Method method) {
        return ClassUtils.isSetter(method);
    }
}
