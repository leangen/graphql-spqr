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

import graphql.execution.batched.Batched;
import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.execution.MethodInvoker;
import io.leangen.graphql.metadata.execution.SingletonMethodInvoker;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

/**
 * A resolver builder that exposes all public methods
 */
public class PublicResolverBuilder extends FilteredResolverBuilder {

    @SuppressWarnings("WeakerAccess")
    public PublicResolverBuilder(String basePackage) {
        this.basePackage = basePackage;
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
        if (rawType.isArray() || rawType.isPrimitive()) return Collections.emptyList();
        return Arrays.stream(rawType.getMethods())
                .filter(REAL_ONLY)
                .filter(method -> isPackageAcceptable(method, rawType))
                .filter(this::isQuery)
                .filter(filters.stream().reduce(Predicate::and).orElse(ACCEPT_ALL))
                .map(method -> new Resolver(
                        operationNameGenerator.generateQueryName(method, beanType, querySourceBean),
                        operationNameGenerator.generateQueryName(method, beanType, querySourceBean),
                        method.isAnnotationPresent(Batched.class),
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.buildResolverArguments(method, beanType),
                        method.isAnnotationPresent(GraphQLComplexity.class) ? method.getAnnotation(GraphQLComplexity.class).value() : null
                ))
                .collect(Collectors.toList());
    }

    private Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType, List<Predicate<Member>> filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        if (rawType.isArray()|| rawType.isPrimitive()) return Collections.emptyList();
        return Arrays.stream(rawType.getMethods())
                .filter(REAL_ONLY)
                .filter(method -> isPackageAcceptable(method, rawType))
                .filter(this::isMutation)
                .filter(filters.stream().reduce(Predicate::and).orElse(ACCEPT_ALL))
                .map(method -> new Resolver(
                        operationNameGenerator.generateMutationName(method, beanType, querySourceBean),
                        operationNameGenerator.generateMutationName(method, beanType, querySourceBean),
                        method.isAnnotationPresent(Batched.class),
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.buildResolverArguments(method, beanType),
                        method.isAnnotationPresent(GraphQLComplexity.class) ? method.getAnnotation(GraphQLComplexity.class).value() : null
                ))
                .collect(Collectors.toList());
    }

    protected boolean isQuery(Method method) {
        return !isMutation(method);
    }

    protected boolean isMutation(Method method) {
        return method.getReturnType() == void.class;
    }

    private boolean isPackageAcceptable(Method method, Class<?> beanType) {
        String basePackage = Utils.notEmpty(this.basePackage) ? this.basePackage : beanType.getPackage().getName();
        return method.getDeclaringClass().equals(beanType) || method.getDeclaringClass().getPackage().getName().startsWith(basePackage);
    }
}
