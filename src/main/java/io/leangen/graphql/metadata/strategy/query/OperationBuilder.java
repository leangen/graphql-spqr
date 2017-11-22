package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.Type;
import java.util.List;

import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface OperationBuilder {

    Operation buildQuery(Type context, List<Resolver> resolvers);
    Operation buildMutation(Type context, List<Resolver> resolvers);
    Operation buildSubscription(Type context, List<Resolver> resolvers);
}
