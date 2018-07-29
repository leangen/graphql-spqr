package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.messages.MessageBundle;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface OperationBuilder {

    Operation buildQuery(Type context, List<Resolver> resolvers, MessageBundle messageBundle);
    Operation buildMutation(Type context, List<Resolver> resolvers, MessageBundle messageBundle);
    Operation buildSubscription(Type context, List<Resolver> resolvers, MessageBundle messageBundle);
}
