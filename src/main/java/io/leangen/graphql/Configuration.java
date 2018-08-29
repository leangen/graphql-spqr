package io.leangen.graphql;

import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.ScalarDeserializationStrategy;

@SuppressWarnings("WeakerAccess")
public class Configuration {
    public final InterfaceMappingStrategy interfaceMappingStrategy;
    public final ScalarDeserializationStrategy scalarDeserializationStrategy;
    public final TypeTransformer typeTransformer;
    public final String[] basePackages;

    Configuration(InterfaceMappingStrategy interfaceMappingStrategy, ScalarDeserializationStrategy scalarDeserializationStrategy, TypeTransformer typeTransformer, String[] basePackages) {
        this.interfaceMappingStrategy = interfaceMappingStrategy;
        this.scalarDeserializationStrategy = scalarDeserializationStrategy;
        this.typeTransformer = typeTransformer;
        this.basePackages = basePackages;
    }
}
