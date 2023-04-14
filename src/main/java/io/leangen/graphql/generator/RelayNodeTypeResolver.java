package io.leangen.graphql.generator;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.util.GraphQLUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
class RelayNodeTypeResolver extends DelegatingTypeResolver {

    RelayNodeTypeResolver(GlobalEnvironment environment) {
        super(GraphQLUtils.NODE, environment);
    }
}
