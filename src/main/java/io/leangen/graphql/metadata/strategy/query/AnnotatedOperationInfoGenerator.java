package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;

import java.util.Optional;
import java.util.function.Function;

/**
 * Created by bojan.tomic on 7/3/16.
 */
public class AnnotatedOperationInfoGenerator implements OperationInfoGenerator {

    @Override
    public String name(OperationInfoGeneratorParams params) {
        return map(params, GraphQLQuery::name, GraphQLMutation::name, GraphQLSubscription::name);
    }

    @Override
    public String description(OperationInfoGeneratorParams params) {
        return map(params, GraphQLQuery::description, GraphQLMutation::description, GraphQLSubscription::description);
    }

    @Override
    public String deprecationReason(OperationInfoGeneratorParams params) {
        return map(params, GraphQLQuery::deprecationReason, GraphQLMutation::deprecationReason, GraphQLSubscription::deprecationReason);
    }

    private String map(OperationInfoGeneratorParams params,
                       Function<GraphQLQuery, String> queryMapper,
                       Function<GraphQLMutation, String> mutationMapper,
                       Function<GraphQLSubscription, String> subscriptionMapper) {
        switch (params.getOperationType()) {
            case QUERY:
                return Optional.ofNullable(params.getElement().getAnnotation(GraphQLQuery.class))
                        .map(queryMapper)
                        .orElse(null);
            case MUTATION:
                return Optional.ofNullable(params.getElement().getAnnotation(GraphQLMutation.class))
                        .map(mutationMapper)
                        .orElse(null);
            case SUBSCRIPTION:
                return Optional.ofNullable(params.getElement().getAnnotation(GraphQLSubscription.class))
                        .map(subscriptionMapper)
                        .orElse(null);
            default: return null;
        }
    }
}
