package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.ClassUtils;

public interface ResolverBuilder {

    Predicate<Member> ACCEPT_ALL = member -> true;
    Predicate<Member> REAL_ONLY = member -> (!(member instanceof Method) || !((Method) member).isBridge()) && !member.isSynthetic();
    Predicate<Member> NOT_IGNORED = member -> !(member instanceof AnnotatedElement && ClassUtils.hasAnnotation((AnnotatedElement) member, GraphQLIgnore.class));

    Collection<Resolver> buildQueryResolvers(Object querySourceBean, AnnotatedType beanType);
    Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType);
    Collection<Resolver> buildSubscriptionResolvers(Object querySourceBean, AnnotatedType beanType);
}
