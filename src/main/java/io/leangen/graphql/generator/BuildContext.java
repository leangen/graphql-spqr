package io.leangen.graphql.generator;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import graphql.relay.Relay;
import graphql.schema.TypeResolver;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

@SuppressWarnings("WeakerAccess")
public class BuildContext {

    public final GlobalEnvironment globalEnvironment;
    public final OperationRepository operationRepository;
    public final TypeRepository typeRepository;
    public final TypeMapperRepository typeMappers;
    public final Relay relay;
    public final TypeResolver typeResolver;
    public final InterfaceMappingStrategy interfaceStrategy;
    public final ValueMapperFactory valueMapperFactory;
    public final InputFieldDiscoveryStrategy inputFieldStrategy;
    public final TypeInfoGenerator typeInfoGenerator;
    public final RelayMappingConfig relayMappingConfig;

    public final Set<String> knownTypes;
    public final Set<String> knownInputTypes;
    public final Map<Type, Set<Type>> abstractComponentTypes = new HashMap<>();

    /**
     *
     * @param operationRepository Repository that can be used to fetch all known (singleton and domain) queries
     * @param typeMappers Repository of all registered {@link io.leangen.graphql.generator.mapping.TypeMapper}s
     * @param converters Repository of all registered {@link io.leangen.graphql.generator.mapping.InputConverter}s
     *                   and {@link io.leangen.graphql.generator.mapping.OutputConverter}s
     */
    public BuildContext(OperationRepository operationRepository, TypeMapperRepository typeMappers,
                        ConverterRepository converters, ArgumentInjectorRepository inputProviders,
                        InterfaceMappingStrategy interfaceStrategy, TypeInfoGenerator typeInfoGenerator,
                        ValueMapperFactory valueMapperFactory, InputFieldDiscoveryStrategy inputFieldStrategy,
                        Set<String> knownTypes, Set<String> knownInputTypes, RelayMappingConfig relayMappingConfig) {
        this.operationRepository = operationRepository;
        this.typeRepository = new TypeRepository();
        this.typeMappers = typeMappers;
        this.typeInfoGenerator = typeInfoGenerator;
        this.relay = new Relay();
        this.typeResolver = new DelegatingTypeResolver(this.typeRepository, typeInfoGenerator);
        this.interfaceStrategy = interfaceStrategy;
        this.valueMapperFactory = valueMapperFactory;
        this.inputFieldStrategy = inputFieldStrategy;
        this.globalEnvironment = new GlobalEnvironment(relay, typeRepository, converters, inputProviders);
        this.knownTypes = knownTypes;
        this.knownInputTypes = knownInputTypes;
        this.relayMappingConfig = relayMappingConfig;
    }
}
