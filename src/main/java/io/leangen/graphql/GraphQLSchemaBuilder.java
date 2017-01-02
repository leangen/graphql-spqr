package io.leangen.graphql;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.QueryRepository;
import io.leangen.graphql.generator.QuerySourceRepository;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.common.ArrayMapper;
import io.leangen.graphql.generator.mapping.common.CollectionToListOutputConverter;
import io.leangen.graphql.generator.mapping.common.EnumMapper;
import io.leangen.graphql.generator.mapping.common.InterfaceMapper;
import io.leangen.graphql.generator.mapping.common.ListMapper;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.generator.mapping.common.NonNullMapper;
import io.leangen.graphql.generator.mapping.common.ObjectTypeMapper;
import io.leangen.graphql.generator.mapping.common.OptionalAdapter;
import io.leangen.graphql.generator.mapping.common.PageMapper;
import io.leangen.graphql.generator.mapping.common.RelayIdMapper;
import io.leangen.graphql.generator.mapping.common.ScalarMapper;
import io.leangen.graphql.generator.mapping.common.StreamToCollectionTypeAdapter;
import io.leangen.graphql.generator.mapping.common.UnionInlineMapper;
import io.leangen.graphql.generator.mapping.common.UnionTypeMapper;
import io.leangen.graphql.generator.mapping.common.VoidToBooleanTypeAdapter;
import io.leangen.graphql.generator.mapping.strategy.AnnotatedInterfaceStrategy;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.input.GsonInputDeserializerFactory;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverExtractor;
import io.leangen.graphql.metadata.strategy.query.DefaultQueryBuilder;
import io.leangen.graphql.metadata.strategy.query.QueryBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverExtractor;

import static graphql.schema.GraphQLObjectType.newObject;
import static java.util.Collections.addAll;

/**
 * <p>This class is the main entry point to the library. It is used to generate a GraphQL schema by analyzing the registered classes
 * and exposing the chosen methods as GraphQL queries or mutations. The process of choosing the methods to expose is delegated
 * to {@link ResolverExtractor} instances, and a different set of extractors can be attached to each registered class.
 * One such coupling of a registered class and a set of extractors is modeled by an instance of {@link io.leangen.graphql.generator.QuerySource}.
 * Methods of the {@code with*QuerySource} family are used to register sources to be analyzed.</p>
 * <p>Extractors can also be registered globally (to be used when none are provided explicitly) via {@link #withResolverExtractors(ResolverExtractor...)}.
 * The process of mapping the Java methods to GraphQL queries/mutations will also transparently map all encountered Java types
 * to corresponding GraphQL types. The entire mapping process is handled by an instance {@link QueryGenerator} where actual type
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
 * GraphQLSchema schema = new GraphQLSchemaBuilder()
 *      .withSingletonQuerySource(userService) //register a query source and use the default extractor
 *      .withDomainQuerySource(User.class, new BeanResolverExtractor()) //customize how queries are extracted from User.class
 *      .build();
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
public class GraphQLSchemaBuilder {

    private InterfaceMappingStrategy interfaceStrategy = new AnnotatedInterfaceStrategy();
    private QueryBuilder queryBuilder = new DefaultQueryBuilder(new GsonInputDeserializerFactory());
    private final QuerySourceRepository querySourceRepository = new QuerySourceRepository();
    private final Collection<GraphQLSchemaProcessor> processors = new HashSet<>();
    private final ConverterRepository converterRepository = new ConverterRepository();
    private final TypeMapperRepository typeMappers = new TypeMapperRepository();

    /**
     * Default constructor
     */
    public GraphQLSchemaBuilder() {}

    /**
     * Construct with {@code querySourceBeans} as singleton query sources with default extractors
     * <p>Equivalent to: {@code new GraphQLSchemaBuilder().withSingletonQuerySources(querySourceBeans)}</p>
     * <p>See {@link #withSingletonQuerySources(Object...)}</p>
     *
     * @param querySourceBeans Singletons to register as query sources
     */
    public GraphQLSchemaBuilder(Object... querySourceBeans) {
        this.withSingletonQuerySources(querySourceBeans);
    }

    /**
     * Register {@code querySourceBean} as a singleton {@link io.leangen.graphql.generator.QuerySource},
     * with its class (obtained via {@link Object#getClass()}) as its runtime type and with the globally registered
     * {@link ResolverExtractor}s.
     * All query/mutation methods discovered by analyzing the {@code querySourceBean}'s type will be later,
     * in query resolution time, invoked on this specific instance (hence the 'singleton' in the method name).
     * Instances of stateless service classes are commonly registered this way.
     *
     * @param querySourceBean The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean) {
        return withSingletonQuerySource(querySourceBean, querySourceBean.getClass());
    }

    /**
     * Register {@code querySourceBean} as a singleton {@link io.leangen.graphql.generator.QuerySource},
     * with {@code beanType} as its runtime type and with the globally registered {@link ResolverExtractor}s.
     * <p>See {@link #withSingletonQuerySource(Object)}</p>
     *
     * @param querySourceBean The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code querySourceBean}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}). This is commonly the case when the class is generic
     *                 or when the instance has been proxied by a framework.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, Type beanType) {
        return withSingletonQuerySource(querySourceBean, GenericTypeReflector.annotate(beanType));
    }

    /**
     * Same as {@link #withSingletonQuerySource(Object, Type)}, except that an {@link AnnotatedType} is used as
     * {@code querySourceBean}'s runtime type. Needed when type annotations such as {@link io.leangen.graphql.annotations.NonNull}
     * not directly declared on the class should be captured.
     *
     * @param querySourceBean The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code querySourceBean}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}) and when annotations on the type should be kept.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get an {@link AnnotatedType} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, AnnotatedType beanType) {
        this.querySourceRepository.registerSingletonQuerySource(querySourceBean, beanType);
        return this;
    }

    /**
     * Same as {@link #withSingletonQuerySource(Object, Type)} except that custom {@link ResolverExtractor}s will be used
     * to look through {@code beanType} for methods to be exposed.
     *
     * @param querySourceBean The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code querySourceBean}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}). This is commonly the case when the class is generic
     *                 or when the instance has been proxied by a framework.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get a {@link Type} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param extractors Custom extractor to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, Type beanType, ResolverExtractor... extractors) {
        return withSingletonQuerySource(querySourceBean, GenericTypeReflector.annotate(beanType), extractors);
    }

    /**
     * Same as {@link #withSingletonQuerySource(Object, AnnotatedType)} except that custom {@link ResolverExtractor}s will be used
     * to look through {@code beanType} for methods to be exposed.
     *
     * @param querySourceBean The singleton bean whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     * @param beanType Runtime type of {@code querySourceBean}. Should be explicitly provided when it differs from its class
     *                 (that can be obtained via {@link Object#getClass()}) and when annotations on the type should be kept.
     *                 Use {@link io.leangen.geantyref.TypeToken} to get an {@link AnnotatedType} literal
     *                 or {@link io.leangen.geantyref.TypeFactory} to create it dynamically.
     * @param extractors Custom extractors to use when analyzing {@code beanType}
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, AnnotatedType beanType, ResolverExtractor... extractors) {
        this.querySourceRepository.registerSingletonQuerySource(querySourceBean, beanType, Arrays.asList(extractors));
        return this;
    }

    /**
     * Same as {@link #withSingletonQuerySource(Object)} except that multiple beans can be registered at the same time.
     *
     * @param querySourceBeans Singleton beans whose type is to be scanned for query/mutation methods and on which
     *                        those methods will be invoked in query/mutation execution time
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withSingletonQuerySources(Object... querySourceBeans) {
        Arrays.stream(querySourceBeans).forEach(this::withSingletonQuerySource);
        return this;
    }

    /**
     * Register {@code domainQuerySource} type to be scanned for exposed methods, using the globally registered extractors.
     * This is not normally required as domain types will be discovered dynamically and globally registered extractors
     * will be used anyway. Only needed when no exposed method refers to this domain type directly
     * (relying exclusively on interfaces instead) and the type should still be mapped and listed in the resulting schema.
     * <p><b>Note: Interface mapping is not yet fully implemented!</b></p>
     *
     * @param domainQuerySource The domain type that is to be scanned for query/mutation methods
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDomainQuerySource(Type domainQuerySource) {
        return withDomainQuerySource(GenericTypeReflector.annotate(domainQuerySource));
    }

    /**
     * The same as {@link #withDomainQuerySource(Type)} except that an {@link AnnotatedType} is used,
     * so any extra annotations on the type (not only those directly on the class) are kept.
     *
     * @param domainQuerySource The domain type that is to be scanned for query/mutation methods
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDomainQuerySource(AnnotatedType domainQuerySource) {
        this.querySourceRepository.registerDomainQuerySource(domainQuerySource);
        return this;
    }

    /**
     * Register {@code domainQuerySource} type to be scanned for exposed methods, using the provided {@link ResolverExtractor}s.
     * Domain types are discovered dynamically, when referred to by an exposed method (either as its parameter type or return type).
     * This method gives a way to customize how the discovered domain type will be analyzed.
     *
     * @param domainQuerySource The domain type that is to be scanned for query/mutation methods
     * @param extractors Custom extractors to use when analyzing {@code domainQuerySource} type
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDomainQuerySource(Type domainQuerySource, ResolverExtractor... extractors) {
        return withDomainQuerySource(GenericTypeReflector.annotate(domainQuerySource), extractors);
    }

    /**
     * Same as {@link #withDomainQuerySource(Type, ResolverExtractor...)} except that an {@link AnnotatedType} is used
     * so any extra annotations on the type (not only those directly on the class) are kept.
     *
     * @param domainQuerySource The annotated domain type that is to be scanned for query/mutation methods
     * @param extractors Custom extractors to use when analyzing {@code domainQuerySource} type
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDomainQuerySource(AnnotatedType domainQuerySource, ResolverExtractor... extractors) {
        this.querySourceRepository.registerDomainQuerySource(domainQuerySource, Arrays.asList(extractors));
        return this;
    }

    /**
     * Same as {@link #withDomainQuerySource(Type)} except that multiple types can be registered at the same time
     *
     * @param domainQuerySources The domain types that are to be scanned for query/mutation methods
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDomainQuerySources(Type... domainQuerySources) {
        Arrays.stream(domainQuerySources).forEach(this::withDomainQuerySource);
        return this;
    }

    /**
     * Globally registers {@link ResolverExtractor}s to be used for sources that don't have explicitly assigned extractors.
     *
     * @param resolverExtractors Extractors to be globally registered
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withResolverExtractors(ResolverExtractor... resolverExtractors) {
        this.querySourceRepository.registerGlobalQueryExtractors(resolverExtractors);
        return this;
    }

    /**
     * Registers all built-in {@link TypeMapper}s, {@link InputConverter}s and {@link OutputConverter}s
     * <p>Equivalent to calling {@code withDefaultResolverExtractors().withDefaultMappers().withDefaultConverters()}</p>
     * <p>See {@link #withDefaultMappers()} and {@link #withDefaultConverters()}</p>
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDefaults() {
        return withDefaultResolverExtractors()
                .withDefaultMappers()
                .withDefaultConverters();
    }

    /**
     * Registers default resolver extractors. Currently this only includes {@link AnnotatedResolverExtractor}.
     * <p>See {@link #withResolverExtractors(ResolverExtractor...)}</p>
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDefaultResolverExtractors() {
        return withResolverExtractors(new AnnotatedResolverExtractor());
    }

    public GraphQLSchemaBuilder withInterfaceMappingStrategy(InterfaceMappingStrategy interfaceStrategy) {
        this.interfaceStrategy = interfaceStrategy;
        return this;
    }

    /**
     * Registers all built-in {@link TypeMapper}s
     * <p>See {@link #withTypeMappers(TypeMapper...)}</p>
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDefaultMappers() {
        ObjectTypeMapper objectTypeMapper = new ObjectTypeMapper();
        return withTypeMappers(
                new NonNullMapper(), new RelayIdMapper(), new ScalarMapper(), new EnumMapper(), new ArrayMapper<>(),
                new UnionTypeMapper(), new UnionInlineMapper(), new StreamToCollectionTypeAdapter(),
                new MapToListTypeAdapter<>(), new VoidToBooleanTypeAdapter(), new ListMapper(), new PageMapper(),
                new OptionalAdapter(), new InterfaceMapper(interfaceStrategy, objectTypeMapper), objectTypeMapper);
    }

    /**
     * Registers all built-in {@link InputConverter}s and {@link OutputConverter}s
     * <p>See {@link #withInputConverters(InputConverter[])} and {@link #withOutputConverters(OutputConverter[])} )}</p>
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withDefaultConverters() {
        return withInputConverters(new MapToListTypeAdapter<>(), new OptionalAdapter(), new StreamToCollectionTypeAdapter())
                .withOutputConverters(new MapToListTypeAdapter<>(), new VoidToBooleanTypeAdapter(),
                        new CollectionToListOutputConverter(), new OptionalAdapter(), new StreamToCollectionTypeAdapter());
    }

    /**
     * Registers custom {@link TypeMapper}s to be used for mapping Java type to GraphQL types.
     * <p><b>Ordering of mappers is strictly important as the first {@link TypeMapper} that supports the given Java type
     * will be used for mapping it.</b></p>
     * <p>See {@link TypeMapper#supports(AnnotatedType)}</p>
     *
     * @param typeMappers Custom type mappers to register with the builder
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withTypeMappers(TypeMapper... typeMappers) {
        this.typeMappers.registerTypeMappers(typeMappers);
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
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withInputConverters(InputConverter<?,?>... inputConverters) {
        this.converterRepository.registerConverters(inputConverters);
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
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withOutputConverters(OutputConverter<?,?>... outputConverters) {
        this.converterRepository.registerConverters(outputConverters);
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
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withTypeAdapters(AbstractTypeAdapter<?,?>... typeAdapters) {
        withInputConverters((InputConverter<?, ?>[]) typeAdapters);
        withOutputConverters((OutputConverter<?, ?>[]) typeAdapters);
        return withTypeMappers((TypeMapper[]) typeAdapters);
    }

    /**
     * Registers custom schema processors that can perform arbitrary transformations on the schema just before it is built.
     *
     * @param processors Custom processors to call right before the GraphQL schema is built
     *
     * @return This {@link GraphQLSchemaBuilder} instance, to allow method chaining
     */
    public GraphQLSchemaBuilder withSchemaProcessors(GraphQLSchemaProcessor... processors) {
        addAll(this.processors, processors);
        return this;
    }

    /**
     * Registers default extractors, mappers and/or converters if none were registered explicitly,
     * ensuring the builder is in a valid state
     */
    private void init() {
        if (querySourceRepository.isEmpty()) {
            throw new IllegalStateException("At least one (non-domain) query source must be registered");
        }
        if (!querySourceRepository.hasGlobalExtractors()) {
            withDefaultResolverExtractors();
        }
        if (typeMappers.isEmpty()) {
            withDefaultMappers();
        }
        if (converterRepository.isEmpty()) {
            withDefaultConverters();
        }
    }

    /**
     * Builds the GraphQL schema based on the results of analysis of the registered sources. All exposed methods will be mapped
     * as queries or mutation and all Java types referred to by those methods will be mapped to corresponding GraphQL types.
     * Such schema can then be used to construct {@link graphql.GraphQL} instances. See the example in the description of this class.
     *
     * @return The finished GraphQL schema
     */
    public GraphQLSchema build() {
        init();

        QueryRepository queryRepository = new QueryRepository(querySourceRepository, queryBuilder);
        BuildContext buildContext = new BuildContext(queryRepository, typeMappers, converterRepository, interfaceStrategy);
        QueryGenerator queryGenerator = new QueryGenerator(buildContext);

        GraphQLSchema.Builder builder = GraphQLSchema.newSchema();
        builder
                .query(newObject()
                        .name("QUERY_ROOT")
                        .description("Enclosing type for queries")
                        .fields(queryGenerator.getQueries())
                        .build())
                .mutation(newObject()
                        .name("MUTATION_ROOT")
                        .description("Enclosing type for mutations")
                        .fields(queryGenerator.getMutations())
                        .build());
        applyProcessors(builder);

        return builder.build();
    }

    private void applyProcessors(GraphQLSchema.Builder builder) {
        for (GraphQLSchemaProcessor processor : processors) {
            processor.process(builder);
        }
    }
}
