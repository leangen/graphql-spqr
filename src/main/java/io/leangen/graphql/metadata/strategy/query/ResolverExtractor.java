package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.QueryResolver;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Created by bojan.tomic on 6/7/16.
 */
public interface ResolverExtractor {

    Predicate<Member> acceptAll = method -> true;

    Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType);
    Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType);
    Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType, Predicate<Member>... filters);
    Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType, Predicate<Member>... filters);
}
