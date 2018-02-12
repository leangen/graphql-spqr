package io.leangen.graphql.extension;

import io.leangen.graphql.GraphQLSchemaGenerator;

public interface Module {

    void setUp(SetupContext context);

    interface SetupContext {
        GraphQLSchemaGenerator getSchemaGenerator();
    }
}
