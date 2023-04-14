package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.Resolver;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.function.Predicate;

public interface ResolverBuilder {

    Predicate<Member> ACCEPT_ALL = member -> true;

    Collection<Resolver> buildQueryResolvers(ResolverBuilderParams params);
    Collection<Resolver> buildMutationResolvers(ResolverBuilderParams params);
    Collection<Resolver> buildSubscriptionResolvers(ResolverBuilderParams params);

    default boolean supports(AnnotatedType type) {
        return true;
    }
}
