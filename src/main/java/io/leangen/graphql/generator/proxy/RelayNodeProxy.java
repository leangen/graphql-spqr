package io.leangen.graphql.generator.proxy;

import java.lang.reflect.InvocationHandler;

/**
 * Created by bojan.tomic on 4/24/16.
 */
public interface RelayNodeProxy {
    void setDelegate(InvocationHandler target);

    String getGraphQLTypeHint();

    void setGraphQLTypeHint(String graphQLTypeHint);
}