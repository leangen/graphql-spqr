package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.query.execution.FieldAccessor;
import io.leangen.graphql.query.execution.MethodInvoker;
import io.leangen.graphql.query.execution.SingletonMethodInvoker;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by bojan.tomic on 6/7/16.
 */
public class AnnotatedResolverExtractor implements ResolverExtractor {

    private final QueryNameGenerator queryNameGenerator;
    private final QueryResolverArgumentExtractor argumentExtractor;

    public AnnotatedResolverExtractor() {
        this(new DelegatingQueryNameGenerator(new AnnotatedQueryNameGenerator(), new MethodQueryNameGenerator()), new AnnotatedArgumentExtractor());
    }

    public AnnotatedResolverExtractor(QueryNameGenerator queryNameGenerator, QueryResolverArgumentExtractor argumentExtractor) {
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
        Stream<QueryResolver> methodInvokers = ClassUtils.getAnnotatedMethods(ClassUtils.getRawType(beanType.getType()), GraphQLQuery.class).stream()
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(method -> new QueryResolver(
                        queryNameGenerator.generateQueryName(method, beanType),
                        method.getAnnotation(GraphQLQuery.class).description(),
                        method.getAnnotatedReturnType().isAnnotationPresent(RelayId.class),
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.extractResolverArguments(method, beanType)
                ));
        Stream<QueryResolver> fieldAccessors = ClassUtils.getAnnotatedFields(ClassUtils.getRawType(beanType.getType()), GraphQLQuery.class).stream()
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(field -> new QueryResolver(
                        queryNameGenerator.generateQueryName(field, beanType),
                        field.getAnnotation(GraphQLQuery.class).description(),
                        field.getAnnotatedType().isAnnotationPresent(RelayId.class),
                        new FieldAccessor(field, beanType),
                        Collections.emptyList()
                ));
        return Stream.concat(methodInvokers, fieldAccessors).collect(Collectors.toSet());

    }

    @Override
    public Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType, Predicate<Member>... filters) {
        return ClassUtils.getAnnotatedMethods(ClassUtils.getRawType(beanType.getType()), GraphQLMutation.class).stream()
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(method -> new QueryResolver(
                        queryNameGenerator.generateMutationName(method, beanType),
                        method.getAnnotation(GraphQLMutation.class).description(),
                        method.getAnnotatedReturnType().isAnnotationPresent(RelayId.class),
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        argumentExtractor.extractResolverArguments(method, beanType)
                )).collect(Collectors.toSet());
    }

    @Override
    public int hashCode() {
        return queryNameGenerator.getClass().hashCode() + argumentExtractor.getClass().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AnnotatedResolverExtractor)) return false;
        AnnotatedResolverExtractor that = (AnnotatedResolverExtractor) other;
        return this.queryNameGenerator.getClass().equals(that.queryNameGenerator.getClass())
                && this.argumentExtractor.getClass().equals(that.argumentExtractor.getClass());
    }
}
