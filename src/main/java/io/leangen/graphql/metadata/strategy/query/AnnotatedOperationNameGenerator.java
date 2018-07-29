package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created by bojan.tomic on 7/3/16.
 */
public class AnnotatedOperationNameGenerator implements OperationNameGenerator {

    @Override
    public String generateQueryName(OperationNameGeneratorParams<?> params) {
        return Optional.ofNullable(params.getElement().getAnnotation(GraphQLQuery.class))
                .map(query -> params.getMessageBundle().interpolate(query.name()))
                .orElse(null);
    }

    @Override
    public String generateMutationName(OperationNameGeneratorParams<Method> params) {
        return Optional.ofNullable(params.getElement().getAnnotation(GraphQLMutation.class))
                .map(mutation -> params.getMessageBundle().interpolate(mutation.name()))
                .orElse(null);
    }

    @Override
    public String generateSubscriptionName(OperationNameGeneratorParams<Method> params) {
        return Optional.ofNullable(params.getElement().getAnnotation(GraphQLSubscription.class))
                .map(subscription -> params.getMessageBundle().interpolate(subscription.name()))
                .orElse(null);
    }
}
