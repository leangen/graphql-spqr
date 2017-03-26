package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.execution.MethodInvoker;
import io.leangen.graphql.metadata.execution.SingletonMethodInvoker;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 6/10/16.
 */
public class PublicResolverBuilder extends FilteredResolverBuilder {

    @SuppressWarnings("WeakerAccess")
    public PublicResolverBuilder() {
        this.operationNameGenerator = new MethodOperationNameGenerator();
        this.argumentExtractor = new AnnotatedArgumentBuilder();
    }

    @Override
    public Collection<Resolver> buildQueryResolvers(Object querySourceBean, AnnotatedType beanType) {
        return buildQueryResolvers(querySourceBean, beanType, getFilters());
    }

    @Override
    public Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType) {
        return buildMutationResolvers(querySourceBean, beanType, getFilters());
    }

    private Collection<Resolver> buildQueryResolvers(Object querySourceBean, AnnotatedType beanType, List<Predicate<Member>> filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        if (rawType.isArray()) return Collections.emptyList();
        return Arrays.stream(rawType.getMethods())
                .filter(method -> rawType.getPackage().equals(method.getDeclaringClass().getPackage()))
                .filter(this::isQuery)
                .filter(filters.stream().reduce(Predicate::and).orElse(acceptAll))
                .map(method -> new Resolver(
                        operationNameGenerator.generateQueryName(method, beanType),
                        operationNameGenerator.generateQueryName(method, beanType),
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.buildResolverArguments(method, beanType)
                ))
                .collect(Collectors.toList());
    }

    private Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType, List<Predicate<Member>> filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        if (rawType.isArray()) return Collections.emptyList();
        return Arrays.stream(rawType.getMethods())
                .filter(method -> rawType.getPackage().equals(method.getDeclaringClass().getPackage()))
                .filter(this::isMutation)
                .filter(filters.stream().reduce(Predicate::and).orElse(acceptAll))
                .map(method -> new Resolver(
                        operationNameGenerator.generateMutationName(method, beanType),
                        operationNameGenerator.generateMutationName(method, beanType),
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.buildResolverArguments(method, beanType)
                ))
                .collect(Collectors.toList());
    }

    protected boolean isQuery(Method method) {
        return !isMutation(method);
    }

    protected boolean isMutation(Method method) {
        return method.getReturnType() == void.class;
    }
}
