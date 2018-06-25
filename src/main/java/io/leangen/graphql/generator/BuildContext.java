package io.leangen.graphql.generator;

import graphql.relay.Relay;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.strategy.AbstractInputHandler;
import io.leangen.graphql.generator.mapping.strategy.ImplementationDiscoveryStrategy;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.ScalarDeserializationStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class BuildContext {

    public final GlobalEnvironment globalEnvironment;
    public final OperationRepository operationRepository;
    public final TypeRepository typeRepository;
    public final TypeCache typeCache;
    public final TypeMapperRepository typeMappers;
    public final Relay relay;
    public final GraphQLInterfaceType node; //Node interface, as defined by the Relay GraphQL spec
    public final TypeResolver typeResolver;
    public final InterfaceMappingStrategy interfaceStrategy;
    public final String[] basePackages;
    public final ValueMapperFactory valueMapperFactory;
    public final InputFieldDiscoveryStrategy inputFieldStrategy;
    public final InclusionStrategy inclusionStrategy;
    public final ScalarDeserializationStrategy scalarStrategy;
    public final TypeTransformer typeTransformer;
    public final AbstractInputHandler abstractInputHandler;
    public final ImplementationDiscoveryStrategy implDiscoveryStrategy;
    public final TypeInfoGenerator typeInfoGenerator;
    public final RelayMappingConfig relayMappingConfig;

    final Validator validator;

    /**
     * The shared context accessible throughout the schema generation process
     * @param basePackages The base (root) package of the entire project
     * @param environment The globally shared environment
     * @param operationRepository Repository that can be used to fetch all known (singleton and domain) queries
     * @param typeMappers Repository of all registered {@link io.leangen.graphql.generator.mapping.TypeMapper}s
     * @param valueMapperFactory The factory used to produce {@link ValueMapper} instances
     * @param typeInfoGenerator Generates type name/description
     * @param interfaceStrategy The strategy deciding what Java type gets mapped to a GraphQL interface
     * @param scalarStrategy The strategy deciding how abstract Java types are discovered
     * @param abstractInputHandler The strategy deciding what Java type gets mapped to a GraphQL interface
     * @param inputFieldStrategy The strategy deciding how GraphQL input fields are discovered from Java types
     * @param relayMappingConfig Relay specific configuration
     * @param knownTypes The cache of known type names
     */
    public BuildContext(String[] basePackages, GlobalEnvironment environment, OperationRepository operationRepository,
                        TypeMapperRepository typeMappers, ValueMapperFactory valueMapperFactory,
                        TypeInfoGenerator typeInfoGenerator, InterfaceMappingStrategy interfaceStrategy,
                        ScalarDeserializationStrategy scalarStrategy, TypeTransformer typeTransformer, AbstractInputHandler abstractInputHandler,
                        InputFieldDiscoveryStrategy inputFieldStrategy, InclusionStrategy inclusionStrategy,
                        RelayMappingConfig relayMappingConfig, Set<GraphQLType> knownTypes, List<Set<AnnotatedType>> typeAliasGroups,
                        ImplementationDiscoveryStrategy implementationStrategy) {
        this.operationRepository = operationRepository;
        this.typeRepository = environment.typeRepository;
        this.typeCache = new TypeCache(knownTypes);
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
        this.inclusionStrategy = inclusionStrategy;
        this.scalarStrategy = scalarStrategy;
        this.typeTransformer = typeTransformer;
        this.implDiscoveryStrategy = implementationStrategy;
        this.abstractInputHandler = abstractInputHandler;
        this.globalEnvironment = environment;
        this.relayMappingConfig = relayMappingConfig;
        this.validator = new Validator(environment, typeMappers, knownTypes, typeAliasGroups);
    }

    ValueMapper createValueMapper(Stream<AnnotatedType> inputTypes) {
        List<Class> abstractTypes = inputTypes
                .flatMap(input -> abstractInputHandler.findConstituentAbstractTypes(input, this).stream().map(ClassUtils::getRawType))
                .distinct()
                .collect(Collectors.toList());
        Map<Class, List<Class>> concreteSubTypes = abstractTypes.stream()
                .collect(Collectors.toMap(Function.identity(), abs -> abstractInputHandler.findConcreteSubTypes(abs, this)));
        return valueMapperFactory.getValueMapper(concreteSubTypes, globalEnvironment);
    }

    void resolveTypeReferences() {
        typeCache.resolveTypeReferences(typeRepository);
    }
}
