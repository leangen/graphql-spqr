package io.leangen.graphql;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.OperationRepository;
import io.leangen.graphql.generator.OperationSource;
import io.leangen.graphql.generator.OperationSourceRepository;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.common.ArrayMapper;
import io.leangen.graphql.generator.mapping.common.CollectionToListOutputConverter;
import io.leangen.graphql.generator.mapping.common.ContextInjector;
import io.leangen.graphql.generator.mapping.common.EnumMapper;
import io.leangen.graphql.generator.mapping.common.EnvironmentInjector;
import io.leangen.graphql.generator.mapping.common.IdAdapter;
import io.leangen.graphql.generator.mapping.common.InputValueDeserializer;
import io.leangen.graphql.generator.mapping.common.InterfaceMapper;
import io.leangen.graphql.generator.mapping.common.ListMapper;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.generator.mapping.common.NonNullMapper;
import io.leangen.graphql.generator.mapping.common.ObjectScalarAdapter;
import io.leangen.graphql.generator.mapping.common.ObjectTypeMapper;
import io.leangen.graphql.generator.mapping.common.OptionalAdapter;
import io.leangen.graphql.generator.mapping.common.PageMapper;
import io.leangen.graphql.generator.mapping.common.RootContextInjector;
import io.leangen.graphql.generator.mapping.common.ScalarMapper;
import io.leangen.graphql.generator.mapping.common.StreamToCollectionTypeAdapter;
import io.leangen.graphql.generator.mapping.common.UnionInlineMapper;
import io.leangen.graphql.generator.mapping.common.UnionTypeMapper;
import io.leangen.graphql.generator.mapping.common.VoidToBooleanTypeAdapter;
import io.leangen.graphql.generator.mapping.strategy.AnnotatedInterfaceStrategy;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.DefaultOperationBuilder;
import io.leangen.graphql.metadata.strategy.query.OperationBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.Defaults;

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
 * GraphQLSchema schema = new GraphQLSchemaGenerator()
 *      .withOperationsFromType(userService) //register an operations source and use the default strategy
 *      .withNestedResolverBuildersForType(User.class, new BeanResolverBuilder()) //customize how queries are extracted from User.class
 *      .generate();
 * GraphQL graphQL = new GraphQL(schema);
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
public class GraphQLSchemaGenerator {

    private InterfaceMappingStrategy interfaceStrategy = new AnnotatedInterfaceStrategy();
    private OperationBuilder operationBuilder = new DefaultOperationBuilder();
    private ValueMapperFactory valueMapperFactory;
    private InputFieldDiscoveryStrategy inputFieldStrategy;
    private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
    private final OperationSourceRepository operationSourceRepository = new OperationSourceRepository();
    private final Collection<GraphQLSchemaProcessor> processors = new HashSet<>();
    private final List<TypeMapper> typeMappers = new ArrayList<>();
    private final List<InputConverter> inputConverters = new ArrayList<>();
    private final List<OutputConverter> outputConverters = new ArrayList<>();
    private final List<ArgumentInjector> argumentInjectors = new ArrayList<>();
    private final Set<GraphQLType> additionalTypes = new HashSet<>();

    private static final String QUERY_ROOT = "QUERY_ROOT";
    private static final String MUTATION_ROOT = "MUTATION_ROOT";

    /**
     * Default constructor
     */
    public GraphQLSchemaGenerator() {}

    /**
     * Construct with {@code serviceSingletons} as singleton query sources with default builders
     * <p>Equivalent to: {@code new GraphQLSchemaGenerator().withOperationsFromType(serviceSingletons)}</p>
     * <p>See {@link #withOperationsFromSingletons(Object...)}</p>
     *
     * @param serviceSingletons Singletons to register as query sources
     */
    public GraphQLSchemaGenerator(Object... serviceSingletons) {
        this.withOperationsFromSingletons(serviceSingletons);
    }

    /**
     * Register {@code serviceSingleton} as a singleton {@link OperationSource},
     * with its class (obtained via {@link Object#getClass()}) as its runtime type and with the globally registered
     * {@link ResolverBuilder}s.
     * All query/mutation methods discovered by analyzing the {@code serviceSingleton}'s type will be later,
     * in query resolution time, invoked on this specific instance (hence the 'singleton' in the method name).
     * Instances of stateless service classes are commonly registered this way.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton) {
        return withOperationsFromSingleton(serviceSingleton, serviceSingleton.getClass());
    }

    /**
     * Register {@code serviceSingleton} as a singleton {@link OperationSource},
     * with {@code beanType} as its runtime type and with the globally registered {@link ResolverBuilder}s.
     * <p>See {@link #withOperationsFromSingleton(Object)}</p>
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}). This is commonly the case when the class is generic
     *                 or when the instance has been proxied by a framework.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, Type beanType) {
        return withOperationsFromSingleton(serviceSingleton, GenericTypeReflector.annotate(beanType));
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, Type)}, except that an {@link AnnotatedType} is used as
     * {@code serviceSingleton}'s runtime type. Needed when type annotations such as {@link GraphQLNonNull}
     * not directly declared on the class should be captured.
     *
     * @param serviceSingleton The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code serviceSingleton}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}) and when annotations on the type should be kept.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get an {@link AnnotatedType} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOperationsFromSingleton(Object serviceSingleton, AnnotatedType beanType) {
        this.operationSourceRepository.registerOperationSource(serviceSingleton, beanType);
        return this;
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, Type)} except that custom {@link ResolverBuilder}s will be used
     * to look through {@code beanType} for methods to be exposed.
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
        return withOperationsFromSingleton(serviceSingleton, GenericTypeReflector.annotate(beanType), builders);
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object, AnnotatedType)} except that custom {@link ResolverBuilder}s will be used
     * to look through {@code beanType} for methods to be exposed.
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
        this.operationSourceRepository.registerOperationSource(serviceSingleton, beanType, Arrays.asList(builders));
        return this;
    }

    /**
     * Same as {@link #withOperationsFromSingleton(Object)} except that multiple beans can be registered at the same time.
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

    public GraphQLSchemaGenerator withOperationsFromType(Type serviceType) {
        return this.withOperationsFromType(GenericTypeReflector.annotate(serviceType));
    }

    public GraphQLSchemaGenerator withOperationsFromType(Type serviceType, ResolverBuilder... builders) {
        return this.withOperationsFromType(GenericTypeReflector.annotate(serviceType), builders);
    }

    public GraphQLSchemaGenerator withOperationsFromTypes(Type... serviceType) {
        Arrays.stream(serviceType).forEach(this::withOperationsFromType);
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromType(AnnotatedType serviceType) {
        this.operationSourceRepository.registerOperationSource(serviceType);
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromType(AnnotatedType serviceType, ResolverBuilder... builders) {
        this.operationSourceRepository.registerOperationSource(serviceType, Arrays.asList(builders));
        return this;
    }

    public GraphQLSchemaGenerator withOperationsFromTypes(AnnotatedType... serviceType) {
        Arrays.stream(serviceType).forEach(this::withOperationsFromType);
        return this;
    }

    /**
     * Register a type to be scanned for exposed methods, using the globally registered builders.
     * This is not normally required as domain types will be discovered dynamically and globally registered builders
     * will be used anyway. Only needed when no exposed method refers to this domain type directly
     * (relying exclusively on interfaces or super-types instead) and the type should still be mapped and listed in the resulting schema.
     *
     * @param types The domain types that are to be scanned for query/mutation methods
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedOperationsFromTypes(Type... types) {
        Arrays.stream(types).forEach(this::withNestedResolverBuildersForType);
        return this;
    }

    /**
     * The same as {@link #withNestedOperationsFromTypes(Type...)} except that an {@link AnnotatedType} is used,
     * so any extra annotations on the type (not only those directly on the class) are kept.
     *
     * @param types The domain types that are to be scanned for query/mutation methods
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedOperationsFromTypes(AnnotatedType... types) {
        Arrays.stream(types).forEach(this::withNestedResolverBuildersForType);
        return this;
    }

    /**
     * Register {@code querySourceType} type to be scanned for exposed methods, using the provided {@link ResolverBuilder}s.
     * Domain types are discovered dynamically, when referred to by an exposed method (either as its parameter type or return type).
     * This method gives a way to customize how the discovered domain type will be analyzed.
     *
     * @param querySourceType The domain type that is to be scanned for query/mutation methods
     * @param resolverBuilders Custom resolverBuilders to use when analyzing {@code querySourceType} type
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedResolverBuildersForType(Type querySourceType, ResolverBuilder... resolverBuilders) {
        return withNestedResolverBuildersForType(GenericTypeReflector.annotate(querySourceType), resolverBuilders);
    }

    /**
     * Same as {@link #withNestedResolverBuildersForType(Type, ResolverBuilder...)} except that an {@link AnnotatedType} is used
     * so any extra annotations on the type (not only those directly on the class) are kept.
     *
     * @param querySourceType The annotated domain type that is to be scanned for query/mutation methods
     * @param resolverBuilders Custom resolverBuilders to use when analyzing {@code querySourceType} type
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedResolverBuildersForType(AnnotatedType querySourceType, ResolverBuilder... resolverBuilders) {
        this.operationSourceRepository.registerNestedOperationSource(querySourceType, Arrays.asList(resolverBuilders));
        return this;
    }

    /**
     * Globally registers {@link ResolverBuilder}s to be used for sources that don't have explicitly assigned builders.
     *
     * @param resolverBuilders builders to be globally registered
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withResolverBuilders(ResolverBuilder... resolverBuilders) {
        this.operationSourceRepository.registerGlobalResolverBuilders(resolverBuilders);
        return this;
    }

    /**
     * Globally registers {@link ResolverBuilder}s to be used for sources that don't have explicitly assigned builders.
     *
     * @param resolverBuilders builders to be globally registered
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withNestedResolverBuilders(ResolverBuilder... resolverBuilders) {
        this.operationSourceRepository.registerGlobalNestedResolverBuilders(resolverBuilders);
        return this;
    }

    /**
     * Registers all built-in {@link TypeMapper}s, {@link InputConverter}s and {@link OutputConverter}s
     * <p>Equivalent to calling {@code withDefaultResolverBuilders().withDefaultMappers().withDefaultConverters()}</p>
     * <p>See {@link #withDefaultMappers()} and {@link #withDefaultConverters()}</p>
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withDefaults() {
        return withDefaultResolverBuilders()
                .withDefaultMappers()
                .withDefaultConverters()
                .withDefaultArgumentInjectors();
    }

    /**
     * Registers default resolver builders. Currently this only includes {@link AnnotatedResolverBuilder}.
     * <p>See {@link #withResolverBuilders(ResolverBuilder...)}</p>
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withDefaultResolverBuilders() {
        return withResolverBuilders(new AnnotatedResolverBuilder());
    }

    /**
     * Registers default resolver builders. Currently this only includes {@link AnnotatedResolverBuilder}.
     * <p>See {@link #withResolverBuilders(ResolverBuilder...)}</p>
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withDefaultNestedResolverBuilders() {
        return withNestedResolverBuilders(new AnnotatedResolverBuilder(), new BeanResolverBuilder());
    }

    public GraphQLSchemaGenerator withInterfaceMappingStrategy(InterfaceMappingStrategy interfaceStrategy) {
        this.interfaceStrategy = interfaceStrategy;
        return this;
    }

    /**
     * Registers custom {@link TypeMapper}s to be used for mapping Java type to GraphQL types.
     * <p><b>Ordering of mappers is strictly important as the first {@link TypeMapper} that supports the given Java type
     * will be used for mapping it.</b></p>
     * <p>See {@link TypeMapper#supports(AnnotatedType)}</p>
     *
     * @param typeMappers Custom type mappers to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withTypeMappers(TypeMapper... typeMappers) {
        addAll(this.typeMappers, typeMappers);
        return this;
    }

    /**
     * Registers custom {@link InputConverter}s to be used for converting values provided by the GraphQL client
     * into those expected by the corresponding Java method. Only needed in some specific cases when usual deserialization
     * isn't enough, for example, when a client-provided {@link java.util.List} should be repackaged into a {@link java.util.Map},
     * which is normally done because GraphQL type system has no direct support for maps.
     * <p><b>Ordering of converters is strictly important as the first {@link InputConverter} that supports the given Java type
     * will be used for converting it.</b></p>
     * <p>See {@link InputConverter#supports(AnnotatedType)}</p>
     * <p>See {@link #withDefaults()}</p>
     *
     * @param inputConverters Custom input converters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withInputConverters(InputConverter<?,?>... inputConverters) {
        addAll(this.inputConverters, inputConverters);
        return this;
    }

    /**
     * Registers custom {@link OutputConverter}s to be used for converting values returned by the exposed Java method
     * into those expected by the GraphQL client. Only needed in some specific cases when usual serialization isn't enough,
     * for example, when an instance of {@link java.util.Map} should be repackaged into a {@link java.util.List}, which
     * is normally done because GraphQL type system has no direct support for maps.
     * <p><b>Ordering of converters is strictly important as the first {@link OutputConverter} that supports the given Java type
     * will be used for converting it.</b></p>
     * <p>See {@link OutputConverter#supports(AnnotatedType)}</p>
     * <p>See {@link #withDefaults()}</p>
     *
     * @param outputConverters Custom output converters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withOutputConverters(OutputConverter<?,?>... outputConverters) {
        addAll(this.outputConverters, outputConverters);
        return this;
    }

    /**
     * Type adapters (instances of {@link AbstractTypeAdapter}) are both type mappers and bi-directional converters,
     * implementing {@link TypeMapper}, {@link InputConverter} and {@link OutputConverter}.
     * They're used in the same way as mappers/converters individually, and exist solely because it can sometimes
     * be convenient to group the logic for mapping and converting to/from the same Java type in one place.
     * For example, because GraphQL type system has no notion of maps, {@link java.util.Map}s require special logic
     * both when mapping them to a GraphQL type and when converting them before and after invoking a Java method.
     * For this reason, all code dealing with translating {@link java.util.Map}s is kept in one place in {@link MapToListTypeAdapter}.
     * <p><b>Ordering of mappers/converters is strictly important as the first one supporting the given Java type
     * will be used to map/convert it.</b></p>
     * <p>See {@link #withDefaultMappers()}</p>
     * <p>See {@link #withDefaultConverters()}</p>
     * <p>See {@link #withDefaults()}</p>
     *
     * @param typeAdapters Custom type adapters to register with the builder
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withTypeAdapters(AbstractTypeAdapter<?,?>... typeAdapters) {
        withInputConverters((InputConverter<?, ?>[]) typeAdapters);
        withOutputConverters((OutputConverter<?, ?>[]) typeAdapters);
        return withTypeMappers((TypeMapper[]) typeAdapters);
    }

    public GraphQLSchemaGenerator withArgumentInjectors(ArgumentInjector... argumentInjectors) {
        addAll(this.argumentInjectors, argumentInjectors);
        return this;
    }

    public GraphQLSchemaGenerator withMetaDataGenerator(TypeInfoGenerator typeInfoGenerator) {
        this.typeInfoGenerator = typeInfoGenerator;
        return this;
    }

    public GraphQLSchemaGenerator withValueMapperFactory(ValueMapperFactory valueMapperFactory) {
        this.valueMapperFactory = valueMapperFactory;
        return this;
    }

    public GraphQLSchemaGenerator withInputFieldDiscoveryStrategy(InputFieldDiscoveryStrategy inputFieldStrategy) {
        this.inputFieldStrategy = inputFieldStrategy;
        return this;
    }

    public GraphQLSchemaGenerator withAdditionalTypes(Collection<GraphQLType> additionalTypes) {
        additionalTypes.stream()
                .filter(type -> !isInternalType(type))
                .forEach(this.additionalTypes::add);
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
     * Registers all built-in {@link TypeMapper}s
     * <p>See {@link #withTypeMappers(TypeMapper...)}</p>
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withDefaultMappers() {
        ObjectTypeMapper objectTypeMapper = new ObjectTypeMapper();
        return withTypeMappers(
                new NonNullMapper(), new IdAdapter(), new ScalarMapper(),
                new ObjectScalarAdapter(), new EnumMapper(), new ArrayMapper<>(), new UnionTypeMapper(),
                new UnionInlineMapper(), new StreamToCollectionTypeAdapter(), new MapToListTypeAdapter<>(),
                new VoidToBooleanTypeAdapter(), new ListMapper(), new PageMapper(), new OptionalAdapter(),
                new InterfaceMapper(interfaceStrategy, objectTypeMapper), objectTypeMapper);
    }

    public GraphQLSchemaGenerator withDefaultInputConverters() {
        return withInputConverters(
                new MapToListTypeAdapter<>(), new OptionalAdapter(), new StreamToCollectionTypeAdapter());
    }

    public GraphQLSchemaGenerator withDefaultOutputConverters() {
        return withOutputConverters(
                new IdAdapter(), new ObjectScalarAdapter(), new MapToListTypeAdapter<>(),
                new VoidToBooleanTypeAdapter(), new CollectionToListOutputConverter(),
                new OptionalAdapter(), new StreamToCollectionTypeAdapter());
    }

    /**
     * Registers all built-in {@link InputConverter}s and {@link OutputConverter}s.
     * The equivalent of calling both {@link #withDefaultInputConverters()} and {@link #withDefaultOutputConverters()}.
     * <p>See {@link #withInputConverters(InputConverter[])} and {@link #withOutputConverters(OutputConverter[])} )}</p>
     *
     * @return This {@link GraphQLSchemaGenerator} instance, to allow method chaining
     */
    public GraphQLSchemaGenerator withDefaultConverters() {
        return withDefaultInputConverters().withOutputConverters();
    }

    public GraphQLSchemaGenerator withDefaultArgumentInjectors() {
        return withArgumentInjectors(
                new IdAdapter(), new RootContextInjector(), new ContextInjector(),
                new EnvironmentInjector(), new InputValueDeserializer());
    }

    /**
     * Sets the default values for all settings not configured explicitly,
     * ensuring the builder is in a valid state
     */
    private void init() {
        if (operationSourceRepository.isEmpty()) {
            throw new IllegalStateException("At least one top-level query source must be registered");
        }
        if (!operationSourceRepository.hasGlobalResolverBuilders()) {
            withDefaultResolverBuilders();
        }
        if (!operationSourceRepository.hasGlobalNestedResolverBuilders()) {
            withDefaultNestedResolverBuilders();
        }
        if (typeMappers.isEmpty()) {
            withDefaultMappers();
        }
        if (outputConverters.isEmpty()) {
            withDefaultOutputConverters();
        }
        if (inputConverters.isEmpty()) {
            withDefaultInputConverters();
        }
        if (argumentInjectors.isEmpty()) {
            withDefaultArgumentInjectors();
        }
        if (valueMapperFactory == null) {
            valueMapperFactory = Defaults.valueMapperFactory(typeInfoGenerator);
        }
        if (inputFieldStrategy == null) {
            ValueMapper def = valueMapperFactory.getValueMapper();
            if (def instanceof InputFieldDiscoveryStrategy) {
                inputFieldStrategy = (InputFieldDiscoveryStrategy) def;
            } else {
                inputFieldStrategy = (InputFieldDiscoveryStrategy) Defaults.valueMapperFactory(typeInfoGenerator).getValueMapper();
            }
        }
    }

    /**
     * Generates a GraphQL schema based on the results of analysis of the registered sources. All exposed methods will be mapped
     * as queries or mutations and all Java types referred to by those methods will be mapped to corresponding GraphQL types.
     * Such schema can then be used to construct {@link graphql.GraphQL} instances. See the example in the description of this class.
     *
     * @return A GraphQL schema
     */
    public GraphQLSchema generate() {
        init();

        Set<String> knownTypeNames = this.additionalTypes.stream()
                .filter(type -> type instanceof GraphQLOutputType)
                .map(GraphQLType::getName)
                .collect(Collectors.toSet());

        Set<String> knownInputTypeNames = this.additionalTypes.stream()
                .filter(type -> type instanceof GraphQLInputType)
                .map(GraphQLType::getName)
                .collect(Collectors.toSet());

        BuildContext buildContext = new BuildContext(
                new OperationRepository(operationSourceRepository, operationBuilder),
                new TypeMapperRepository(typeMappers),
                new ConverterRepository(inputConverters, outputConverters),
                new ArgumentInjectorRepository(argumentInjectors),
                interfaceStrategy, typeInfoGenerator, valueMapperFactory, inputFieldStrategy,
                knownTypeNames, knownInputTypeNames);
        OperationMapper operationMapper = new OperationMapper(buildContext);

        GraphQLSchema.Builder builder = GraphQLSchema.newSchema()
                .query(newObject()
                        .name(QUERY_ROOT)
                        .description("Query root type")
                        .fields(operationMapper.getQueries())
                        .build())
                .mutation(newObject()
                        .name(MUTATION_ROOT)
                        .description("Mutation root type")
                        .fields(operationMapper.getMutations())
                        .build());
        applyProcessors(builder);

        return builder.build(additionalTypes);
    }

    private void applyProcessors(GraphQLSchema.Builder builder) {
        for (GraphQLSchemaProcessor processor : processors) {
            processor.process(builder);
        }
    }

    private boolean isInternalType(GraphQLType type) {
        return !type.getName().startsWith("__") && !type.getName().equals(QUERY_ROOT) && !type.getName().equals(MUTATION_ROOT);
    }
}
