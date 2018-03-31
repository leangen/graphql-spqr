package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;

public interface ResolverBuilder {

    Predicate<Member> ACCEPT_ALL = member -> true;
    Predicate<Member> REAL_ONLY = member -> (!(member instanceof Method) || !((Method) member).isBridge()) && !member.isSynthetic();

    Collection<Resolver> buildQueryResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy);
    Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy);
    Collection<Resolver> buildSubscriptionResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy);
}
