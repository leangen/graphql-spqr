package io.leangen.graphql.metadata.strategy.query;

import java.util.List;

import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface OperationBuilder {

    Operation buildQuery(List<Resolver> resolvers);
    Operation buildMutation(List<Resolver> resolvers);
}
