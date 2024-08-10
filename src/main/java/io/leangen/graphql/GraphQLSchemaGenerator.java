package io.leangen.graphql;

import graphql.relay.Relay;
import graphql.schema.*;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolverInterceptor;
import io.leangen.graphql.execution.ResolverInterceptorFactory;
import io.leangen.graphql.execution.ResolverInterceptorFactoryParams;
import io.leangen.graphql.generator.*;
import io.leangen.graphql.generator.mapping.SchemaTransformer;
import io.leangen.graphql.generator.mapping.*;
import io.leangen.graphql.generator.mapping.common.*;
import io.leangen.graphql.generator.mapping.core.CompletableFutureAdapter;
import io.leangen.graphql.generator.mapping.core.DataFetcherResultAdapter;
import io.leangen.graphql.generator.mapping.core.PublisherAdapter;
import io.leangen.graphql.generator.mapping.strategy.*;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.messages.DelegatingMessageBundle;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.DefaultInclusionStrategy;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.query.*;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.metadata.strategy.value.*;
import io.leangen.graphql.module.Module;
import io.leangen.graphql.util.*;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLObjectType.newObject;
import static java.util.Collections.addAll;

/**
 * <p>This class is the main entry point to the library. It is used to generate a GraphQL schema by analyzing the registered classes
 * and exposing the chosen methods as GraphQL queries or mutations. The process of choosing the methods to expose is delegated
 * to {@link ResolverBuilder} instances, and a different set of builders can be attached to each registered class.
 * One such coupling of a registered class and a set of builders is modeled by an instance of {@link OperationSource}.
 * Methods of the {@code with*OperationSource} family are used to register sources to be analyzed.</p>
 * <p>Builders can also be registered globally (to be used when none are provided explicitly) via {@link #withResolverBuilders(ResolverBuilder...)}.
 * The process of mapping the Java methods to GraphQL queries/mutations will also transparently map all encountered Java types
 * to corresponding GraphQL types. The entire mapping process is handled by an instance {@link OperationMapper} where actual type
 * mapping is delegated to different instances of {@link TypeMapper}.</p>
 * <p>To customize the mapping process, clients can registers their own {@link TypeMapper}s using {@link #withTypeMappers(TypeMapper...)}.
 * Runtime conversion between values provided by the GraphQL client and those expected by Java code might be needed.
 * This is handled by {@link InputConverter} instances.</p>
 * <p>Similarly, the conversion between values returned by Java code and those expected by the GraphQL client (if needed) is
 * handled by {@link OutputConverter} instances.
 * Custom implementations of both {@link InputConverter} and {@link OutputConverter} can be provided using
 * {@link #withInputConverters(InputConverter[])} and {@link #withOutputConverters(OutputConverter[])} respectively.</p>
 *
 * <p><b>Example:</b></p>
 *
 * <pre>
 * {@code
 * UserService userService = new UserService(); //could also be injected by a framework
 * ExecutableSchema schema = new GraphQLSchemaGenerator()
 *      .withOperationsFromSingletons(userService) //register an operations source and use the default strategy
 *      .withNestedResolverBuildersForType(User.class, new BeanResolverBuilder()) //customize how queries are extracted from User.class
 *      .generateExecutable();
 * GraphQL graphQL = GraphQLRuntime.newGraphQL(schema).build();
 *
 * //keep the reference to GraphQL instance and execute queries against it.
 * //this query selects a user by ID and requests name and regDate fields only
 * ExecutionResult result = graphQL.execute(
 * "{ user (id: 123) {
 *      name,
 *      regDate
 *  }}");
 *  }
 * </pre>
 */
@SuppressWarnings({"WeakerAccess", "rawtypes", "UnusedReturnValue", "unused"})
public class GraphQLSchemaGenerator implements GeneratorConfigurer<GraphQLSchemaGenerator> {

    private InterfaceMappingStrategy interfaceStrategy = new AnnotatedInterfaceStrategy();
    private ScalarDeserializationStrategy scalarStrategy;
    private AbstractInputHandler abstractInputHandler = new NoOpAbstractInputHandler();
    private OperationBuilder operationBuilder = new DefaultOperationBuilder(DefaultOperationBuilder.TypeInference.NONE);
    private DirectiveBuilder directiveBuilder = new AnnotatedDirectiveBuilder();
    private ValueMapperFactory<?> valueMapperFactory;
    private InclusionStrategy inclusionStrategy;
    private ImplementationDiscoveryStrategy implDiscoveryStrategy = new DefaultImplementationDiscoveryStrategy();
    private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
    private TypeTransformer typeTransformer = new DefaultTypeTransformer(false, false);
    private GlobalEnvironment environment;
    private String[] basePackages = Utils.emptyArray();
    private final DelegatingMessageBundle messageBundle = new DelegatingMessageBundle();
    private Executor batchLoaderExecutor;
    private List<TypeMapper> typeMappers;
    private List<SchemaTransformer> transformers;
    private Comparator<AnnotatedType> typeComparator;
    private List<InputFieldBuilder> inputFieldBuilders;
    private ResolverInterceptorFactory interceptorFactory;
    private JavaDeprecationMappingConfig javaDeprecationConfig = new JavaDeprecationMappingConfig(true, "Deprecated");
    private final OperationSourceRegistry operationSourceRegistry = new OperationSourceRegistry();
    private final List<ExtensionProvider<GeneratorConfiguration, TypeMapper>> typeMapperProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, SchemaTransformer>> schemaTransformerProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, InputConverter>> inputConverterProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, OutputConverter>> outputConverterProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ArgumentInjector>> argumentInjectorProviders = new ArrayList<>();
    private final List<ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder>> inputFieldBuilderProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ResolverBuilder>> resolverBuilderProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ResolverBuilder>> nestedResolverBuilderProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, Module>> moduleProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory>> interceptorFactoryProviders = new ArrayList<>();
    private final List<ExtensionProvider<GeneratorConfiguration, Comparator<AnnotatedType>>> typeComparatorProviders = new ArrayList<>();
    private final Collection<GraphQLSchemaProcessor> processors = new HashSet<>();
    private final RelayMappingConfig relayMappingConfig = new RelayMappingConfig();
    private final Map<String, GraphQLDirective> additionalDirectives = new HashMap<>();
    private final List<AnnotatedType> additionalDirectiveTypes = new ArrayList<>();
    private final GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
    private final Map<String, GraphQLNamedType> additionalTypes = new HashMap<>();
    private final Map<String, AnnotatedType> additionalTypeMappings = new HashMap<>();

    private final String queryRoot;
    private final String mutationRoot;
    private final String subscriptionRoot;
    private final String queryRootDescription;
    private final String mutationRootDescription;
    private final String subscriptionRootDescription;

    /**
     * Default constructor
     */
    public GraphQLSchemaGenerator() {
        this("Query", "Mutation", "Subscription");
    }

    /**
     * Constructor which allows to customize names of root types.
     * @param queryRoot name of query root type
     * @param mutationRoot name of mutation root type
     * @param subscriptionRoot name of subscription root type
     */
    public GraphQLSchemaGenerator(String queryRoot, String mutationRoot, String subscriptionRoot) {
        this(queryRoot, "Query root", mutationRoot, "Mutation root", subscriptionRoot, "Subscription root");
    }

    /**
     * Constructor which allows to customize names of root types.
     * @param queryRoot name of query root type
     * @param mutationRoot name of mutation root type
     * @param subscriptionRoot name of subscription root type
     */
    public GraphQLSchemaGenerator(String queryRoot, String queryRootDescription, String mutationRoot, String mutationRootDescription, String subscriptionRoot, String subscriptionRootDescription) {
        this.queryRoot = queryRoot;
        this.mutationRoot = mutationRoot;
        this.subscriptionRoot = subscriptionRoot;
        this.queryRootDescription = queryRootDescription;
        this.mutationRootDescription = mutationRootDescription;
        this.subscriptionRootDescription = subscriptionRootDescription;
    }

    /**
     * Register {@code serviceSingleton} as a singleton {@link OperationSource},
     * with its class (obtained via {@link Object#getClass()}) as its runtime type, using the provided
     * {@link ResolverBuilder}s to look for methods to be exposed or the globally registered
     * {@link ResolverBuilder}s if none are provided.
     * All query/mutation methods discovered by analyzing the {@code serviceSingleton}'s type will be later,
     * in query resolution time, invoked on this specific instance (hence the 'singleton' in the method name).
     * Instances of stateless service classes are commonly registered this way.
     *
     * @implNote Injection containers (like Spring or CDI) will often wrap managed bean instances into proxies,
     * making it difficult to reliably detect their type. For this reason, it is recommended in such cases to use
     * a different overload of this method and provide the type explicitly.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param builders Custom strategy to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, ResolverBuilder... builders) {
        return withOperationsFromSingleton(serviceSingleton, serviceSingleton.getClass(), builders);
    }

    /**
     * Register {@code serviceSingleton} as a singleton {@link OperationSource}, with {@code beanType}
     * as its static type, using the provided {@link ResolverBuilder}s to look for methods to be exposed
     * or the globally registered {@link ResolverBuilder}s if none are provided.
     * All query/mutation methods discovered by analyzing the {@code beanType} will be later,
     * in query resolution time, invoked on this specific instance (hence the 'singleton' in the method name).
     * Instances of stateless service classes are commonly registered this way.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}). This is commonly the case when the class is generic
     *                 or when the instance has been proxied by a framework.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param builders Custom strategy to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, Type beanType, ResolverBuilder... builders) {
        return withOperationsFromSingleton(serviceSingleton, GenericTypeReflector.annotate(checkType(beanType)), builders);
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, Type, ResolverBuilder...)}, except that an {@link AnnotatedType} is used as
     * {@code serviceSingleton}'s static type. Needed when type annotations such as {@link GraphQLNonNull}
     * not directly declared on the class should be captured.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}) and when annotations on the type should be kept.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get an {@link AnnotatedType} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param builders Custom builders to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, AnnotatedType beanType, ResolverBuilder... builders) {
        return withOperationsFromBean(() -> serviceSingleton, beanType, null, builders);
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, ResolverBuilder...)} except that multiple beans
     * can be registered at the same time.
     *
     * @param serviceSingletons Singleton beans whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingletons(Object... serviceSingletons) {
        Arrays.stream(serviceSingletons).forEach(this::withOperationsFromSingleton);
        return this;
    }

    /**
     * Analyzes {@code beanType} using the provided {@link ResolverBuilder}s to look for methods to be exposed
     * or the globally registered {@link ResolverBuilder}s if none are provided, and uses {@code serviceSupplier}
     * to obtain an instance on which query/mutation methods are invoked at runtime.
     * Container managed beans (of any scope) are commonly registered this way.
     *
     * @param serviceSupplier The supplier that will be used to obtain an instance on which the exposed methods
     *                        will be invoked when resolving queries/mutations/subscriptions.
     * @param beanType Static type of instances provided by {@code serviceSupplier}.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param builders Custom strategy to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromBean(Supplier<Object> serviceSupplier, Type beanType, ResolverBuilder... builders) {
        return withOperationsFromBean(serviceSupplier, GenericTypeReflector.annotate(checkType(beanType)), ClassUtils.getRawType(beanType), builders);
    }

    /**
     * Same as {@link #withOperationsFromBean(Supplier, Type, ResolverBuilder...)}, except that an {@link AnnotatedType}
     * is used as the static type of the instances provided by {@code serviceSupplier}.
     * Needed when type annotations such as {@link GraphQLNonNull} not directly declared on the class should be captured.
     */
    public GraphQLSchemaGenerator withOperationsFromBean(Supplier<Object> serviceSupplier, AnnotatedType beanType, ResolverBuilder... builders) {
        return withOperationsFromBean(serviceSupplier, beanType, ClassUtils.getRawType(beanType.getType()),  builders);
    }

    /**
     * Same as {@link #withOperationsFromBean(Supplier, Type, ResolverBuilder...)}, but the actual runtime type of
     * the instances provided by {@code serviceSupplier} will be used to choose the method to invoke at runtime.
     * This is the absolute safest approach to registering beans, and is needed when the instances are proxied
     * by a container (e.g. Spring, CDI or others) and can _not_ be cast to {@code beanType} at runtime.
     *
     * @param serviceSupplier The supplier that will be used to obtain an instance on which the exposed methods
     *                        will be invoked when resolving queries/mutations/subscriptions.
     * @param beanType Static type of instances provided by {@code serviceSupplier}.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param exposedType Runtime type of the instances provided by {@code serviceSupplier},
     *                    not necessarily possible to cast to {@code beanType}
     * @param builders Custom strategy to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromBean(Supplier<Object> serviceSupplier, Type beanType, Class<?> exposedType, ResolverBuilder... builders) {
        return withOperationsFromBean(serviceSupplier, GenericTypeReflector.annotate(checkType(beanType)), exposedType, builders);
    }

    /**
     * Same as {@link #withOperationsFromBean(Supplier, Type, Class, ResolverBuilder...)}, except that an {@link AnnotatedType}
     * is used as the static type of the instances provided by {@code serviceSupplier}.
     * Needed when type annotations such as {@link GraphQLNonNull} not directly declared on the class should be captured.
     */
    public GraphQLSchemaGenerator withOperationsFromBean(Supplier<Object> serviceSupplier, AnnotatedType beanType, Class<?> exposedType, ResolverBuilder... builders) {
        checkType(beanType);
        this.operationSourceRegistry.registerOperationSource(serviceSupplier, beanType, exposedType, Utils.asList(builders));
        return this;
    }

    /**
     * Analyzes {@code serviceType} using the provided {@link ResolverBuilder}s to look for methods to be exposed
     * or the globally registered {@link ResolverBuilder}s if none are provided.
     * An instance of {@code serviceType} on which the exposed methods are invoked at runtime must be explicitly
     * provided as GraphQL {@code root} for each execution. See {@link graphql.ExecutionInput.Builder#root(Object)}.
     *
     * @param serviceType Type to analyze for methods to expose.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param builders Custom strategy to use when analyzing {@code serviceType}
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromType(Type serviceType, ResolverBuilder... builders) {
        return this.withOperationsFromType(GenericTypeReflector.annotate(serviceType), builders);
    }

    public GraphQLSchemaGenerator withOperationsFromTypes(Type... serviceType) {
        Arrays.stream(serviceType).forEach(this::withOperationsFromType);
        return this;
    }

    /**
     * Same as {@link #withOperationsFromType(Type, ResolverBuilder...)}, except that an {@link AnnotatedType} is used.
     * Needed when type annotations such as {@link GraphQLNonNull} not directly declared on the class should be captured.
     */
    public GraphQLSchemaGenerator withOperationsFromType(AnnotatedType serviceType, ResolverBuilder... builders) {
        checkType(serviceType);
        this.operationSourceRegistry.registerOperationSource(serviceType, Utils.asList(builders));
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromTypes(AnnotatedType... serviceType) {
        Arrays.stream(serviceType).forEach(this::withOperationsFromType);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider) {
        this.resolverBuilderProviders.add(provider);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withNestedResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider) {
        this.nestedResolverBuilderProviders.add(provider);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withInputFieldBuilders(ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder> provider) {
        this.inputFieldBuilderProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withAbstractInputTypeResolution() {
        this.abstractInputHandler = new AutoScanAbstractInputHandler();
        return this;
    }

    public GraphQLSchemaGenerator withAbstractInputHandler(AbstractInputHandler abstractInputHandler) {
        this.abstractInputHandler = abstractInputHandler;
        return this;
    }

    public GraphQLSchemaGenerator withBasePackages(String... basePackages) {
        this.basePackages = Utils.emptyIfNull(basePackages);
        return this;
    }

    public GraphQLSchemaGenerator withStringInterpolation(MessageBundle... messageBundles) {
        this.messageBundle.withBundles(messageBundles);
        return this;
    }

    public GraphQLSchemaGenerator withJavaDeprecationRespected(boolean respectJavaDeprecation) {
        this.javaDeprecationConfig = new JavaDeprecationMappingConfig(respectJavaDeprecation, javaDeprecationConfig.deprecationReason);
        return this;
    }

    public GraphQLSchemaGenerator withJavaDeprecationReason(String deprecationReason) {
        this.javaDeprecationConfig = new JavaDeprecationMappingConfig(javaDeprecationConfig.enabled, deprecationReason);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator) {
        this.typeInfoGenerator = typeInfoGenerator;
        return this;
    }

    public GraphQLSchemaGenerator withValueMapperFactory(ValueMapperFactory valueMapperFactory) {
        this.valueMapperFactory = valueMapperFactory;
        return this;
    }

    public GraphQLSchemaGenerator withInterfaceMappingStrategy(InterfaceMappingStrategy interfaceStrategy) {
        this.interfaceStrategy = interfaceStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withScalarDeserializationStrategy(ScalarDeserializationStrategy scalarStrategy) {
        this.scalarStrategy = scalarStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withInclusionStrategy(InclusionStrategy inclusionStrategy) {
        this.inclusionStrategy = inclusionStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withImplementationDiscoveryStrategy(ImplementationDiscoveryStrategy implDiscoveryStrategy) {
        this.implDiscoveryStrategy = implDiscoveryStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withTypeTransformer(TypeTransformer transformer) {
        this.typeTransformer = transformer;
        return this;
    }

    /**
     * Registers custom {@link TypeMapper}s to be used for mapping Java type to GraphQL types.
     * <p><b>Ordering of mappers is strictly important as the first {@link TypeMapper} that supports the given Java type
     * will be used for mapping it.</b></p>
     * <p>See {@link TypeMapper#supports(java.lang.reflect.AnnotatedElement, AnnotatedType)}</p>
     *
     * @param provider Provides the customized list of TypeMappers to use
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    @Override
    public GraphQLSchemaGenerator withTypeMappers(ExtensionProvider<GeneratorConfiguration, TypeMapper> provider) {
        this.typeMapperProviders.add(provider);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withSchemaTransformers(ExtensionProvider<GeneratorConfiguration, SchemaTransformer> provider) {
        this.schemaTransformerProviders.add(provider);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withInputConverters(ExtensionProvider<GeneratorConfiguration, InputConverter> provider) {
        this.inputConverterProviders.add(provider);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withOutputConverters(ExtensionProvider<GeneratorConfiguration, OutputConverter> provider) {
        this.outputConverterProviders.add(provider);
        return this;
    }

    @Override
    public GraphQLSchemaGenerator withArgumentInjectors(ExtensionProvider<GeneratorConfiguration, ArgumentInjector> provider) {
        this.argumentInjectorProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withModules(Module... modules) {
        return withModules((config, current) -> current.append(modules));
    }

    public GraphQLSchemaGenerator withModules(ExtensionProvider<GeneratorConfiguration, Module> provider) {
        this.moduleProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withResolverInterceptors(List<ResolverInterceptor> innerInterceptors,
                                                           List<ResolverInterceptor> outerInterceptors) {
        return withResolverInterceptorFactories((config, current) -> current.append(
                new GlobalResolverInterceptorFactory(innerInterceptors, outerInterceptors)));
    }

    public GraphQLSchemaGenerator withResolverInterceptors(ResolverInterceptor... interceptors) {
        return withResolverInterceptors(Arrays.asList(interceptors));
    }

    public GraphQLSchemaGenerator withResolverInterceptors(List<ResolverInterceptor> interceptors) {
        return withResolverInterceptorFactories((config, current) -> current.append(GlobalResolverInterceptorFactory.inner(interceptors)));
    }

    public GraphQLSchemaGenerator withOuterResolverInterceptors(ResolverInterceptor... interceptors) {
        return withOuterResolverInterceptors(Arrays.asList(interceptors));
    }

    public GraphQLSchemaGenerator withOuterResolverInterceptors(List<ResolverInterceptor> interceptors) {
        return withResolverInterceptorFactories((config, current) -> current.append(GlobalResolverInterceptorFactory.outer(interceptors)));
    }

    @Override
    public GraphQLSchemaGenerator withResolverInterceptorFactories(ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory> provider) {
        this.interceptorFactoryProviders.add(provider);
        return this;
    }

    @Deprecated
    public GraphQLSchemaGenerator withAdditionalTypes(Collection<GraphQLType> additionalTypes) {
        return withAdditionalTypes(additionalTypes, Collections.emptyMap(), new NoOpCodeRegistryBuilder());
    }

    public GraphQLSchemaGenerator withAdditionalTypes(Collection<? extends GraphQLType> additionalTypes,
                                                      Map<String, AnnotatedType> additionalTypeMappings,
                                                      GraphQLCodeRegistry codeRegistry) {
        return withAdditionalTypes(additionalTypes, additionalTypeMappings, new CodeRegistryMerger(codeRegistry));
    }

    public GraphQLSchemaGenerator withAdditionalTypes(Collection<? extends GraphQLType> additionalTypes,
                                                      Map<String, AnnotatedType> additionalTypeMappings,
                                                      CodeRegistryBuilder codeRegistryUpdater) {
        this.additionalTypeMappings.putAll(additionalTypeMappings);
        additionalTypes.forEach(type -> merge(type, this.additionalTypes, codeRegistryUpdater, this.codeRegistry));
        return this;
    }

    public GraphQLSchemaGenerator withBatchLoaderExecutor(Executor executor) {
        this.batchLoaderExecutor = executor;
        return this;
    }

    private void merge(GraphQLType type, Map<String, GraphQLNamedType> additionalTypes, CodeRegistryBuilder updater, GraphQLCodeRegistry.Builder builder) {
        GraphQLNamedType namedType = GraphQLUtils.unwrap(type);
        if (!isRealType(namedType)) {
            return;
        }
        if (additionalTypes.containsKey(namedType.getName())) {
            if (additionalTypes.get(namedType.getName()).equals(namedType)) {
                return;
            }
            throw new ConfigurationException("Type name collision: multiple registered additional types are named '" + namedType.getName() + "'");
        }
        additionalTypes.put(namedType.getName(), namedType);

        if (namedType instanceof GraphQLInterfaceType) {
            TypeResolver typeResolver = updater.getTypeResolver((GraphQLInterfaceType) namedType);
            if (typeResolver != null) {
                builder.typeResolverIfAbsent((GraphQLInterfaceType) namedType, typeResolver);
            }
        }
        if (namedType instanceof GraphQLUnionType) {
            TypeResolver typeResolver = updater.getTypeResolver((GraphQLUnionType) namedType);
            if (typeResolver != null) {
                builder.typeResolverIfAbsent((GraphQLUnionType) namedType, typeResolver);
            }
        }
        if (namedType instanceof GraphQLFieldsContainer) {
            GraphQLFieldsContainer fieldsContainer = (GraphQLFieldsContainer) namedType;
            fieldsContainer.getFieldDefinitions().forEach(fieldDef -> {
                DataFetcher<?> dataFetcher = updater.getDataFetcher(fieldsContainer, fieldDef);
                if (dataFetcher != null) {
                    builder.dataFetcherIfAbsent(FieldCoordinates.coordinates(fieldsContainer, fieldDef), dataFetcher);
                }
                merge(fieldDef.getType(), additionalTypes, updater, builder);

                fieldDef.getArguments().forEach(arg -> merge(arg.getType(), additionalTypes, updater, builder));
            });
        }
        if (namedType instanceof GraphQLInputFieldsContainer) {
            ((GraphQLInputFieldsContainer) namedType).getFieldDefinitions()
                    .forEach(fieldDef -> merge(fieldDef.getType(), additionalTypes, updater, builder));
        }
    }

    public GraphQLSchemaGenerator withAdditionalDirectives(Type... additionalDirectives) {
        return withAdditionalDirectives(
                Arrays.stream(additionalDirectives).map(GenericTypeReflector::annotate).toArray(AnnotatedType[]::new));
    }

    public GraphQLSchemaGenerator withAdditionalDirectives(AnnotatedType... additionalDirectives) {
        Collections.addAll(this.additionalDirectiveTypes, additionalDirectives);
        return this;
    }

    public GraphQLSchemaGenerator withAdditionalDirectives(GraphQLDirective... additionalDirectives) {
        CodeRegistryBuilder noOp = new NoOpCodeRegistryBuilder();
        Arrays.stream(additionalDirectives)
                .forEach(directive -> {
                    if (this.additionalDirectives.put(directive.getName(), directive) != null) {
                        throw new ConfigurationException("Directive name collision: multiple registered additional directives are named '" + directive.getName() + "'");
                    }
                    directive.getArguments().forEach(arg -> merge(arg.getType(), this.additionalTypes, noOp, this.codeRegistry));
                });
        return this;
    }

    @SafeVarargs
    public final GraphQLSchemaGenerator withTypeComparators(Comparator<AnnotatedType>... comparators) {
        return withTypeComparators((config, current) -> current.append(comparators));
    }

    @Override
    public GraphQLSchemaGenerator withTypeComparators(ExtensionProvider<GeneratorConfiguration, Comparator<AnnotatedType>> provider) {
        this.typeComparatorProviders.add(provider);
        return this;
    }

    public GraphQLSchemaGenerator withOperationBuilder(OperationBuilder operationBuilder) {
        this.operationBuilder = operationBuilder;
        return this;
    }

    public GraphQLSchemaGenerator withDirectiveBuilder(DirectiveBuilder directiveBuilder) {
        this.directiveBuilder = directiveBuilder;
        return this;
    }

    /**
     * Sets a flag that all mutations should be mapped in a Relay-compliant way,
     * using the default name and description for output wrapper fields.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayCompliantMutations() {
        return withRelayCompliantMutations("result", "Mutation result");
    }

    /**
     * Sets a flag signifying that all mutations should be mapped in a Relay-compliant way,
     * using the default name and description for output wrapper fields.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayCompliantMutations(String wrapperFieldName, String wrapperFieldDescription) {
        this.relayMappingConfig.relayCompliantMutations = true;
        this.relayMappingConfig.wrapperFieldName = wrapperFieldName;
        this.relayMappingConfig.wrapperFieldDescription = wrapperFieldDescription;
        return this;
    }

    /**
     * Sets the flag controlling whether the Node interface (as defined by the Relay spec) should be automatically
     * inferred for types that have an ID field.
     *
     * @param enabled Whether the inference should be enabled
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayNodeInterfaceInference(boolean enabled) {
        this.relayMappingConfig.inferNodeInterface = enabled;
        return this;
    }

    /**
     * Removes the requirement on queries returning a Connection to comply with the Relay Connection spec
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withRelayConnectionCheckRelaxed() {
        this.relayMappingConfig.strictConnectionSpec = false;
        return this;
    }

    /**
     * Registers custom schema processors that can perform arbitrary transformations on the schema just before it is built.
     *
     * @param processors Custom processors to call right before the GraphQL schema is built
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withSchemaProcessors(GraphQLSchemaProcessor... processors) {
        addAll(this.processors, processors);
        return this;
    }

    /**
     * Sets the default values for all settings not configured explicitly,
     * ensuring the builder is in a valid state
     */
    private void init() {
        GeneratorConfiguration configuration = new GeneratorConfiguration(interfaceStrategy, scalarStrategy, typeTransformer, basePackages, javaDeprecationConfig);

        //Modules must go first to get a chance to change other settings
        List<Module> modules = Defaults.modules();
        for (ExtensionProvider<GeneratorConfiguration, Module> provider : moduleProviders) {
            modules = provider.getExtensions(configuration, new ExtensionList<>(modules));
        }
        checkForDuplicates("modules", modules);
        Module.SetupContext setupContext = new ModuleConfigurer();
        modules.forEach(module -> module.setUp(setupContext));

        if (operationSourceRegistry.isEmpty()) {
            throw new IllegalStateException("At least one top-level operation source must be registered");
        }

        if (inclusionStrategy == null) {
            inclusionStrategy = new DefaultInclusionStrategy(basePackages);
        }
        ValueMapperFactory internalValueMapperFactory = valueMapperFactory != null
                ? valueMapperFactory
                : Defaults.valueMapperFactory(typeInfoGenerator);
        if (scalarStrategy == null) {
            if (internalValueMapperFactory instanceof ScalarDeserializationStrategy) {
                scalarStrategy = (ScalarDeserializationStrategy) internalValueMapperFactory;
            } else {
                scalarStrategy = (ScalarDeserializationStrategy) Defaults.valueMapperFactory(typeInfoGenerator);
            }
        }

        List<ResolverBuilder> resolverBuilders = Collections.singletonList(new AnnotatedResolverBuilder());
        for (ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider : resolverBuilderProviders) {
            resolverBuilders = provider.getExtensions(configuration, new ExtensionList<>(resolverBuilders));
        }
        checkForEmptyOrDuplicates("resolver builders", resolverBuilders);
        operationSourceRegistry.registerGlobalResolverBuilders(resolverBuilders);

        List<ResolverBuilder> nestedResolverBuilders = Arrays.asList(
                new AnnotatedResolverBuilder(),
                new BeanResolverBuilder(basePackages).withJavaDeprecation(javaDeprecationConfig),
                new RecordResolverBuilder(basePackages));
        for (ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider : nestedResolverBuilderProviders) {
            nestedResolverBuilders = provider.getExtensions(configuration, new ExtensionList<>(nestedResolverBuilders));
        }
        checkForEmptyOrDuplicates("nested resolver builders", nestedResolverBuilders);
        operationSourceRegistry.registerGlobalNestedResolverBuilders(nestedResolverBuilders);

        ObjectTypeMapper objectTypeMapper = new ObjectTypeMapper();
        PublisherAdapter publisherAdapter = new PublisherAdapter();
        EnumMapper enumMapper = new EnumMapper(javaDeprecationConfig);
        typeMappers = Arrays.asList(
                new NonNullMapper(), new IdAdapter(), new ScalarMapper(), new CompletableFutureAdapter<>(),
                publisherAdapter, new AnnotationMapper(), new OptionalIntAdapter(), new OptionalLongAdapter(), new OptionalDoubleAdapter(),
                enumMapper, new ArrayAdapter(), new UnionTypeMapper(), new UnionInlineMapper(),
                new StreamToCollectionTypeAdapter(), new DataFetcherResultAdapter<>(), new VoidToBooleanTypeAdapter(),
                new ListMapper(), new IterableAdapter<>(), new PageMapper(), new OptionalAdapter(), new EnumMapToObjectTypeAdapter(enumMapper),
                new ObjectScalarMapper(), new InterfaceMapper(interfaceStrategy, objectTypeMapper), objectTypeMapper);
        for (ExtensionProvider<GeneratorConfiguration, TypeMapper> provider : typeMapperProviders) {
            typeMappers = provider.getExtensions(configuration, new ExtensionList<>(typeMappers));
        }
        checkForEmptyOrDuplicates("type mappers", typeMappers);

        transformers = Arrays.asList(new NonNullMapper(), publisherAdapter);
        for (ExtensionProvider<GeneratorConfiguration, SchemaTransformer> provider : schemaTransformerProviders) {
            transformers = provider.getExtensions(configuration, new ExtensionList<>(transformers));
        }
        checkForEmptyOrDuplicates("schema transformers", transformers);

        List<OutputConverter> outputConverters = Arrays.asList(
                new IdAdapter(), new ArrayAdapter(), new CollectionOutputConverter(), new CompletableFutureAdapter<>(),
                new OptionalIntAdapter(), new OptionalLongAdapter(), new OptionalDoubleAdapter(), new OptionalAdapter(),
                new StreamToCollectionTypeAdapter(), publisherAdapter);
        for (ExtensionProvider<GeneratorConfiguration, OutputConverter> provider : outputConverterProviders) {
            outputConverters = provider.getExtensions(configuration, new ExtensionList<>(outputConverters));
        }
        checkForDuplicates("output converters", outputConverters);

        List<InputConverter> inputConverters = Arrays.asList(new CompletableFutureAdapter<>(),
                new StreamToCollectionTypeAdapter(), new IterableAdapter<>(), new EnumMapToObjectTypeAdapter(enumMapper));
        for (ExtensionProvider<GeneratorConfiguration, InputConverter> provider : inputConverterProviders) {
            inputConverters = provider.getExtensions(configuration, new ExtensionList<>(inputConverters));
        }
        checkForDuplicates("input converters", inputConverters);

        List<ArgumentInjector> argumentInjectors = Arrays.asList(
                new IdAdapter(), new RootContextInjector(), new ContextInjector(),
                new EnvironmentInjector(), new DirectiveValueDeserializer(), new InputValueDeserializer());
        for (ExtensionProvider<GeneratorConfiguration, ArgumentInjector> provider : argumentInjectorProviders) {
            argumentInjectors = provider.getExtensions(configuration, new ExtensionList<>(argumentInjectors));
        }
        checkForDuplicates("argument injectors", argumentInjectors);

        List<ResolverInterceptorFactory> interceptorFactories = Arrays.asList(
                new DataFetcherResultAdapter<>(), new BatchLoaderAdapterFactory(batchLoaderExecutor), new VoidToBooleanTypeAdapter());
        for (ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory> provider : this.interceptorFactoryProviders) {
            interceptorFactories = provider.getExtensions(configuration, new ExtensionList<>(interceptorFactories));
        }
        interceptorFactory = new DelegatingResolverInterceptorFactory(interceptorFactories);

        Map<GraphQLNamedType, AnnotatedType> additionalMappedTypes = new HashMap<>();
        for (Map.Entry<String, GraphQLNamedType> entry : additionalTypes.entrySet()) {
            additionalMappedTypes.put(entry.getValue(), additionalTypeMappings.get(entry.getKey()));
        }
        environment = new GlobalEnvironment(messageBundle, new Relay(), new TypeRegistry(additionalMappedTypes),
                new ConverterRegistry(inputConverters, outputConverters), new ArgumentInjectorRegistry(argumentInjectors),
                typeTransformer, inclusionStrategy, typeInfoGenerator);
        ExtendedGeneratorConfiguration extendedConfig = new ExtendedGeneratorConfiguration(configuration, environment);
        valueMapperFactory = new MemoizedValueMapperFactory(environment, internalValueMapperFactory);
        ValueMapper def = valueMapperFactory.getValueMapper(Collections.emptyMap(), environment);

        InputFieldBuilder defaultInputFieldBuilder;
        if (def instanceof InputFieldBuilder) {
            defaultInputFieldBuilder = (InputFieldBuilder) def;
        } else {
            defaultInputFieldBuilder = (InputFieldBuilder) Defaults.valueMapperFactory(typeInfoGenerator).getValueMapper(Collections.emptyMap(), environment);
        }
        inputFieldBuilders = Arrays.asList(new AnnotationInputFieldBuilder(), defaultInputFieldBuilder);
        for (ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder> provider : this.inputFieldBuilderProviders) {
            inputFieldBuilders = provider.getExtensions(extendedConfig, new ExtensionList<>(inputFieldBuilders));
        }
        checkForEmptyOrDuplicates("input field builders", inputFieldBuilders);

        List<Comparator<AnnotatedType>> typeComparators = new ArrayList<>();
        //Only consider leangen annotations except @GraphQLNonNull
        typeComparators.add(new IgnoredAnnotationsTypeComparator().include("io.leangen").exclude(GraphQLNonNull.class));
        Type annotatedTypeComparator = TypeFactory.parameterizedClass(Comparator.class, AnnotatedType.class);
        for (TypeMapper mapper : typeMappers) {
            if (GenericTypeReflector.isSuperType(annotatedTypeComparator, mapper.getClass())) {
                //noinspection unchecked
                typeComparators.add((Comparator<AnnotatedType>) mapper);
            }
        }
        for (ExtensionProvider<GeneratorConfiguration, Comparator<AnnotatedType>> provider : this.typeComparatorProviders) {
            typeComparators = provider.getExtensions(configuration, new ExtensionList<>(typeComparators));
        }
        List<Comparator<AnnotatedType>> finalTypeComparators = typeComparators;
        //noinspection ComparatorMethodParameterNotUsed
        typeComparator = (t1, t2) -> finalTypeComparators.stream().anyMatch(comparator -> comparator.compare(t1, t2) == 0) ? 0 : -1;
    }

    /**
     * Generates a GraphQL schema based on the results of analysis of the registered sources. All exposed methods will be mapped
     * as queries or mutations and all Java types referred to by those methods will be mapped to corresponding GraphQL types.
     * Such schema can then be used to construct {@link graphql.GraphQL} instances. See the example in the description of this class.
     *
     * @return A GraphQL schema
     */
    public GraphQLSchema generate() {
        return generateExecutable().getSchema();
    }

    public ExecutableSchema generateExecutable() {
        init();

        final String queryRootName = messageBundle.interpolate(queryRoot);
        final String mutationRootName = messageBundle.interpolate(mutationRoot);
        final String subscriptionRootName = messageBundle.interpolate(subscriptionRoot);

        BuildContext buildContext = new BuildContext(
                basePackages, environment, new OperationRegistry(operationSourceRegistry, operationBuilder, inclusionStrategy,
                typeTransformer, basePackages, environment), new TypeMapperRegistry(typeMappers),
                new SchemaTransformerRegistry(transformers), valueMapperFactory, interfaceStrategy,
                scalarStrategy, typeTransformer, abstractInputHandler, new DelegatingInputFieldBuilder(inputFieldBuilders),
                interceptorFactory, directiveBuilder, inclusionStrategy, relayMappingConfig, additionalTypes.values(),
                additionalDirectiveTypes, typeComparator, implDiscoveryStrategy, codeRegistry);
        OperationMapper operationMapper = new OperationMapper(queryRootName, mutationRootName, subscriptionRootName, buildContext);

        GraphQLSchema.Builder builder = GraphQLSchema.newSchema();
        builder.query(newObject()
                .name(queryRootName)
                .description(messageBundle.interpolate(queryRootDescription))
                .fields(operationMapper.getQueries())
                .build());

        List<GraphQLFieldDefinition> mutations = operationMapper.getMutations();
        if (!mutations.isEmpty()) {
            builder.mutation(newObject()
                    .name(mutationRootName)
                    .description(messageBundle.interpolate(mutationRootDescription))
                    .fields(mutations)
                    .build());
        }

        List<GraphQLFieldDefinition> subscriptions = operationMapper.getSubscriptions();
        if (!subscriptions.isEmpty()) {
            builder.subscription(newObject()
                    .name(subscriptionRootName)
                    .description(messageBundle.interpolate(subscriptionRootDescription))
                    .fields(subscriptions)
                    .build());
        }

        Set<GraphQLType> additional = new HashSet<>(additionalTypes.values());
        additional.addAll(buildContext.typeRegistry.getDiscoveredTypes());
        builder.additionalTypes(additional);

        builder.additionalDirectives(new HashSet<>(additionalDirectives.values()));
        builder.additionalDirectives(new HashSet<>(operationMapper.getDirectives()));

        builder.withSchemaAppliedDirectives(operationMapper.getSchemaDirectives());

        builder.codeRegistry(buildContext.codeRegistry.build());

        applyProcessors(builder, buildContext);
        buildContext.executePostBuildHooks();
        GraphQLSchema schema = builder.build();
        return new ExecutableSchema(schema, buildContext.typeRegistry, operationMapper.getBatchResolvers(), environment);
    }

    private void applyProcessors(GraphQLSchema.Builder builder, BuildContext buildContext) {
        for (GraphQLSchemaProcessor processor : processors) {
            processor.process(builder, buildContext);
        }
    }

    private boolean isRealType(GraphQLNamedType type) {
        // Reject introspection types
        return !(GraphQLUtils.isIntrospectionType(type)
                // Reject quasi-types
                || type instanceof GraphQLTypeReference
                || type instanceof GraphQLArgument
                || type instanceof GraphQLDirective
                // Reject root types
                || type.getName().equals(messageBundle.interpolate(queryRoot))
                || type.getName().equals(messageBundle.interpolate(mutationRoot))
                || type.getName().equals(messageBundle.interpolate(subscriptionRoot)));
    }

    private Type checkType(Type type) {
        if (type == null) {
            throw TypeMappingException.unknownType();
        }
        Class<?> clazz = ClassUtils.getRawType(type);
        if (ClassUtils.isProxy(clazz)) {
            throw new TypeMappingException("The registered object of type " + clazz.getName() +
                    " appears to be a dynamically generated proxy, so its type can not be reliably determined." +
                    " Provide the type explicitly when registering the bean." +
                    " For details and solutions see " + Urls.Errors.DYNAMIC_PROXIES);
        }
        if (ClassUtils.isMissingTypeParameters(type)) {
            throw new TypeMappingException("The registered object is of generic type " + type.getTypeName() + "." +
                    " Provide the full type explicitly when registering the bean." +
                    " For details and solutions see " + Urls.Errors.TOP_LEVEL_GENERICS);
        }
        return type;
    }

    private void checkType(AnnotatedType type) {
        if (type == null) {
            throw TypeMappingException.unknownType();
        }
        checkType(type.getType());
    }

    private void checkForEmptyOrDuplicates(String extensionType, List<?> extensions) {
        if (extensions.isEmpty()) {
            throw new ConfigurationException("No " + extensionType + "SimpleFieldValidation registered");
        }
        checkForDuplicates(extensionType, extensions);
    }

    private <E> void checkForDuplicates(String extensionType, List<E> extensions) {
        Set<E> seen = new HashSet<>();
        extensions.forEach(element -> {
            if (!seen.add(element)) {
                throw new ConfigurationException("Duplicate " + extensionType + " of type " + element.getClass().getName() + " registered");
            }
        });
    }

    public interface CodeRegistryBuilder {

        default TypeResolver getTypeResolver(GraphQLInterfaceType interfaceType) {
            return null;
        }

        default TypeResolver getTypeResolver(GraphQLUnionType unionType) {
            return null;
        }

        default DataFetcher<?> getDataFetcher(GraphQLFieldsContainer parentType, GraphQLFieldDefinition fieldDef) {
            return null;
        }
    }

    private static class CodeRegistryMerger implements CodeRegistryBuilder {

        private final GraphQLCodeRegistry codeRegistry;

        public CodeRegistryMerger(GraphQLCodeRegistry codeRegistry) {
            this.codeRegistry = codeRegistry;
        }

        @Override
        public TypeResolver getTypeResolver(GraphQLInterfaceType interfaceType) {
            return codeRegistry.getTypeResolver(interfaceType);
        }

        @Override
        public TypeResolver getTypeResolver(GraphQLUnionType unionType) {
            return codeRegistry.getTypeResolver(unionType);
        }

        @Override
        public DataFetcher<?> getDataFetcher(GraphQLFieldsContainer parentType, GraphQLFieldDefinition fieldDef) {
            if (parentType instanceof GraphQLObjectType) {
                return codeRegistry.getDataFetcher((GraphQLObjectType) parentType, fieldDef);
            } else {
                return null;
            }
        }
    }

    private static class NoOpCodeRegistryBuilder implements CodeRegistryBuilder {}

    private static class MemoizedValueMapperFactory implements ValueMapperFactory<ValueMapper> {

        private final ValueMapper defaultValueMapper;
        private final ValueMapperFactory<?> delegate;

        public MemoizedValueMapperFactory(GlobalEnvironment environment, ValueMapperFactory<?> delegate) {
            this.defaultValueMapper = delegate.getValueMapper(Collections.emptyMap(), environment);
            this.delegate = delegate;
        }

        @Override
        public ValueMapper getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
            if (concreteSubTypes.isEmpty() || concreteSubTypes.values().stream().allMatch(List::isEmpty)) {
                return this.defaultValueMapper;
            }
            return delegate.getValueMapper(concreteSubTypes, environment);
        }
    }

    private static class GlobalResolverInterceptorFactory implements ResolverInterceptorFactory {

        private final List<ResolverInterceptor> earlyInterceptors;
        private final List<ResolverInterceptor> lateInterceptors;

        GlobalResolverInterceptorFactory(List<ResolverInterceptor> earlyInterceptors,
                                         List<ResolverInterceptor> lateInterceptors) {
            this.earlyInterceptors = earlyInterceptors;
            this.lateInterceptors = lateInterceptors;
        }

        static GlobalResolverInterceptorFactory inner(List<ResolverInterceptor> interceptors) {
            return new GlobalResolverInterceptorFactory(interceptors, Collections.emptyList());
        }

        static GlobalResolverInterceptorFactory outer(List<ResolverInterceptor> interceptors) {
            return new GlobalResolverInterceptorFactory(Collections.emptyList(), interceptors);
        }

        @Override
        public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
            return earlyInterceptors;
        }

        @Override
        public List<ResolverInterceptor> getOuterInterceptors(ResolverInterceptorFactoryParams params) {
            return lateInterceptors;
        }
    }

    private static class DelegatingResolverInterceptorFactory implements ResolverInterceptorFactory {

        private final List<ResolverInterceptorFactory> delegates;

        private DelegatingResolverInterceptorFactory(List<ResolverInterceptorFactory> delegates) {
            this.delegates = delegates;
        }

        @Override
        public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
            return delegates.stream()
                    .flatMap(delegate -> delegate.getInterceptors(params).stream())
                    .collect(Collectors.toList());
        }

        @Override
        public List<ResolverInterceptor> getOuterInterceptors(ResolverInterceptorFactoryParams params) {
            return delegates.stream()
                    .flatMap(delegate -> delegate.getOuterInterceptors(params).stream())
                    .collect(Collectors.toList());
        }
    }

    private class ModuleConfigurer implements Module.SetupContext {

        @Override
        public Module.SetupContext withTypeMappers(ExtensionProvider<GeneratorConfiguration, TypeMapper> provider) {
            typeMapperProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withInputConverters(ExtensionProvider<GeneratorConfiguration, InputConverter> provider) {
            inputConverterProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withOutputConverters(ExtensionProvider<GeneratorConfiguration, OutputConverter> provider) {
            outputConverterProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withArgumentInjectors(ExtensionProvider<GeneratorConfiguration, ArgumentInjector> provider) {
            argumentInjectorProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider) {
            resolverBuilderProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withNestedResolverBuilders(ExtensionProvider<GeneratorConfiguration, ResolverBuilder> provider) {
            nestedResolverBuilderProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withInputFieldBuilders(ExtensionProvider<ExtendedGeneratorConfiguration, InputFieldBuilder> provider) {
            inputFieldBuilderProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withResolverInterceptorFactories(ExtensionProvider<GeneratorConfiguration, ResolverInterceptorFactory> provider) {
            interceptorFactoryProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withSchemaTransformers(ExtensionProvider<GeneratorConfiguration, SchemaTransformer> provider) {
            schemaTransformerProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator) {
            GraphQLSchemaGenerator.this.withTypeInfoGenerator(typeInfoGenerator);
            return this;
        }

        @Override
        public Module.SetupContext withTypeComparators(ExtensionProvider<GeneratorConfiguration, Comparator<AnnotatedType>> provider) {
            typeComparatorProviders.add(0, provider);
            return this;
        }

        @Override
        public Module.SetupContext withInclusionStrategy(UnaryOperator<InclusionStrategy> strategy) {
            inclusionStrategy = strategy.apply(inclusionStrategy);
            return this;
        }

        @Override
        public Module.SetupContext withRelayMappingConfig(Consumer<RelayMappingConfig> configurer) {
            configurer.accept(relayMappingConfig);
            return this;
        }

        @Override
        public GraphQLSchemaGenerator getSchemaGenerator() {
            return GraphQLSchemaGenerator.this;
        }
    }
}
