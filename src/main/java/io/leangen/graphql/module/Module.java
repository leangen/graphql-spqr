package io.leangen.graphql.module;

import io.leangen.graphql.GeneratorConfigurer;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.generator.RelayMappingConfig;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface Module {

    void setUp(SetupContext context);

    @SuppressWarnings("unused")
    interface SetupContext extends GeneratorConfigurer<SetupContext> {

        SetupContext withInclusionStrategy(UnaryOperator<InclusionStrategy> strategy);

        SetupContext withRelayMappingConfig(Consumer<RelayMappingConfig> configurer);

        /**
         * @deprecated Use the {@code SetupContext} itself as {@code GeneratorConfigurer} directly
         * @return The current schema generator
         */
        @Deprecated
        GraphQLSchemaGenerator getSchemaGenerator();
    }
}
