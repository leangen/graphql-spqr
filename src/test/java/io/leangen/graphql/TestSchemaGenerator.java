package io.leangen.graphql;

import graphql.relay.Relay;
import io.leangen.graphql.metadata.strategy.query.DefaultOperationBuilder;

import java.util.Collections;

/**
 * <b>For testing use only!</b>
 * A schema generator with default configuration useful for testing.
 */
public class TestSchemaGenerator extends GraphQLSchemaGenerator {

    private static final String[] basePackages = new String[] {"io.leangen"};

    public TestSchemaGenerator() {
        withBasePackages(basePackages);
        withAdditionalTypes(Collections.singleton(
                Relay.pageInfoType
        ));
        withOperationBuilder(new DefaultOperationBuilder(DefaultOperationBuilder.TypeInference.LIMITED));
    }
}
