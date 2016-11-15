package io.leangen.graphql.generator.proxy;

import graphql.relay.Relay;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.metadata.QueryResolver;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public interface TypeHintProvider {
    String getGraphQLTypeHint(Object result, DataFetchingEnvironment env, Relay relay, QueryResolver resolver);
}
