package io.leangen.graphql.generator;

import graphql.TypeResolutionEnvironment;

/**
 * Created by bojan.tomic on 5/7/16.
 */
public interface TypeHintProvider {
    String getGraphQLTypeHint(Object result, TypeResolutionEnvironment env);
}
