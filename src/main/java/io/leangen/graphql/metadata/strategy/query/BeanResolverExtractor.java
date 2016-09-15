package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 6/10/16.
 */
public class BeanResolverExtractor extends PublicResolverExtractor {

    public BeanResolverExtractor() {
        super(new BeanQueryNameGenerator(), new AnnotatedArgumentExtractor());
    }

    public BeanResolverExtractor(QueryNameGenerator queryNameGenerator, QueryResolverArgumentExtractor argumentExtractor) {
        super(queryNameGenerator, argumentExtractor);
    }

    @Override
    protected boolean isQuery(Method method) {
        return super.isQuery(method) && ClassUtils.isGetter(method);
    }

    @Override
    protected boolean isMutation(Method method) {
        return super.isMutation(method) && ClassUtils.isSetter(method);
    }
}
