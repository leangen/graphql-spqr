package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.function.Predicate;

import io.leangen.graphql.metadata.Resolver;

public interface ResolverBuilder {

    Predicate<Member> acceptAll = method -> true;

    Collection<Resolver> buildQueryResolvers(Object querySourceBean, AnnotatedType beanType);
    Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType);
}
