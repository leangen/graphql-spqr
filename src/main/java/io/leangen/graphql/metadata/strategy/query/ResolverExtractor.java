package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.function.Predicate;

import io.leangen.graphql.metadata.QueryResolver;

public interface ResolverExtractor {

    Predicate<Member> acceptAll = method -> true;

    Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType);
    Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType);
    Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType, Predicate<Member>... filters);
    Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType, Predicate<Member>... filters);
}
