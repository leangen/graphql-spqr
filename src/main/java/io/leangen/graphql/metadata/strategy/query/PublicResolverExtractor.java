package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.query.execution.MethodInvoker;
import io.leangen.graphql.query.execution.SingletonMethodInvoker;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 6/10/16.
 */
public class PublicResolverExtractor implements ResolverExtractor {

    private final QueryNameGenerator queryNameGenerator;
    private final QueryResolverArgumentExtractor argumentExtractor;

    public PublicResolverExtractor() {
        this(new MethodQueryNameGenerator(), new AnnotatedArgumentExtractor());
    }

    public PublicResolverExtractor(QueryNameGenerator queryNameGenerator, QueryResolverArgumentExtractor argumentExtractor) {
        this.queryNameGenerator = queryNameGenerator;
        this.argumentExtractor = argumentExtractor;
    }

    @Override
    public Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType) {
        return extractQueryResolvers(querySourceBean, beanType, acceptAll);
    }

    @Override
    public Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType) {
        return extractMutationResolvers(querySourceBean, beanType, acceptAll);
    }

    @Override
    public Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType, Predicate<Member>... filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        return Arrays.stream(rawType.getMethods())
                .filter(method -> !rawType.isArray() && rawType.getPackage().equals(method.getDeclaringClass().getPackage()))
                .filter(this::isQuery)
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(method -> new QueryResolver(
                        queryNameGenerator.generateQueryName(method, beanType),
                        queryNameGenerator.generateQueryName(method, beanType),
                        false,
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.extractResolverArguments(method, beanType)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType, Predicate<Member>... filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        return Arrays.stream(rawType.getMethods())
                .filter(method -> rawType.getPackage().equals(method.getDeclaringClass().getPackage()))
                .filter(this::isMutation)
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(method -> new QueryResolver(
                        queryNameGenerator.generateMutationName(method, beanType),
                        queryNameGenerator.generateMutationName(method, beanType),
                        false,
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.extractResolverArguments(method, beanType)
                ))
                .collect(Collectors.toList());
    }

    protected boolean isQuery(Method method) {
        return !isMutation(method);
    }

    protected boolean isMutation(Method method) {
        return method.getReturnType() == Void.class;
    }
}
