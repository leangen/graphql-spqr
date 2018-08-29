package io.leangen.graphql;

import io.leangen.graphql.execution.GlobalEnvironment;

public class ExtendedConfiguration extends Configuration {

    public final GlobalEnvironment environment;

    ExtendedConfiguration(Configuration config, GlobalEnvironment environment) {
        super(config.interfaceMappingStrategy, config.scalarDeserializationStrategy, config.typeTransformer, config.basePackages);
        this.environment = environment;
    }
}
