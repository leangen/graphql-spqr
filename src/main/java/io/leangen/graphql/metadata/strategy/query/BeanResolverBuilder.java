package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.Method;

import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 6/10/16.
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
