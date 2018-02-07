package io.leangen.graphql.generator;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.relay.Relay;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.GraphQLUtils;

@SuppressWarnings("WeakerAccess")
public class BuildContext {

    public final GlobalEnvironment globalEnvironment;
    public final OperationRepository operationRepository;
    public final TypeRepository typeRepository;
    public final TypeMapperRepository typeMappers;
    public final Relay relay;
    public final GraphQLInterfaceType node; //Node interface, as defined by the Relay GraphQL spec
    public final TypeResolver typeResolver;
    public final InterfaceMappingStrategy interfaceStrategy;
    public final String[] basePackages;
    public final ValueMapperFactory valueMapperFactory;
    public final InputFieldDiscoveryStrategy inputFieldStrategy;
    public final TypeInfoGenerator typeInfoGenerator;
    public final RelayMappingConfig relayMappingConfig;
    public final Validator validator;

    public final Set<String> knownTypes;
    public final Set<String> knownInputTypes;
    public final Map<Type, Set<Type>> abstractComponentTypes = new HashMap<>();

    /**
     * The shared context accessible throughout the schema generation process
     *
     * @param operationRepository Repository that can be used to fetch all known (singleton and domain) queries
     * @param typeMappers Repository of all registered {@link io.leangen.graphql.generator.mapping.TypeMapper}s
     * @param environment The globally shared environment
     * @param interfaceStrategy The strategy deciding what Java type gets mapped to a GraphQL interface
     * @param basePackages The base (root) package of the entire project
     * @param typeInfoGenerator Generates type name/description
     * @param valueMapperFactory The factory used to produce {@link io.leangen.graphql.metadata.strategy.value.ValueMapper} instances
     * @param inputFieldStrategy The strategy deciding how GraphQL input fields are discovered from Java types
     * @param knownTypes The cache of known type names
     * @param relayMappingConfig Relay specific configuration
     */
    public BuildContext(OperationRepository operationRepository, TypeMapperRepository typeMappers,
                        GlobalEnvironment environment,
                        InterfaceMappingStrategy interfaceStrategy, String[] basePackages,
                        TypeInfoGenerator typeInfoGenerator, ValueMapperFactory valueMapperFactory,
                        InputFieldDiscoveryStrategy inputFieldStrategy, Set<GraphQLType> knownTypes,
                        RelayMappingConfig relayMappingConfig) {
        this.operationRepository = operationRepository;
        this.typeRepository = environment.typeRepository;
        this.typeMappers = typeMappers;
        this.typeInfoGenerator = typeInfoGenerator;
        this.relay = environment.relay;
        this.node = knownTypes.stream()
                .filter(GraphQLUtils::isRelayNodeInterface)
                .findFirst().map(type -> (GraphQLInterfaceType) type)
                .orElse(relay.nodeInterface(new RelayNodeTypeResolver(this.typeRepository, typeInfoGenerator)));
        this.typeResolver = new DelegatingTypeResolver(this.typeRepository, typeInfoGenerator);
        this.interfaceStrategy = interfaceStrategy;
        this.basePackages = basePackages;
        this.valueMapperFactory = valueMapperFactory;
        this.inputFieldStrategy = inputFieldStrategy;
        this.globalEnvironment = environment;
        this.knownTypes = knownTypes.stream()
                .filter(type -> type instanceof GraphQLOutputType)
                .map(GraphQLType::getName)
                .collect(Collectors.toSet());
        this.knownInputTypes = knownTypes.stream()
                .filter(type -> type instanceof GraphQLInputType)
                .map(GraphQLType::getName)
                .collect(Collectors.toSet());
        this.relayMappingConfig = relayMappingConfig;
        this.validator = new Validator(environment, typeMappers, knownTypes);
    }
}
