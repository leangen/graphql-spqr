package io.leangen.graphql;

import graphql.schema.GraphQLSchema;
import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.QuerySourceRepository;
import io.leangen.graphql.generator.mapping.*;
import io.leangen.graphql.generator.mapping.common.*;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverExtractor;
import io.leangen.graphql.metadata.strategy.query.ResolverExtractor;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static graphql.schema.GraphQLObjectType.newObject;
import static java.util.Collections.addAll;

/**
 * This class is the main entry point to the library. It is used to generate a GraphQL schema by analyzing the registered classes
 * and exposing the chosen methods as queries or mutations. The process of choosing the methods to expose is delegated
 * to {@code QueryExtractor}s
 */
public class GraphQLSchemaBuilder {

    private final QuerySourceRepository querySourceRepository = new QuerySourceRepository();
    private final Collection<GraphQLSchemaProcessor> processors = new HashSet<>();
    private final ConverterRepository converterRepository = new ConverterRepository();
    private final TypeMapperRepository typeMappers = new TypeMapperRepository();

    public GraphQLSchemaBuilder() {
        withResolverExtractors(new AnnotatedResolverExtractor());
    }

    public GraphQLSchemaBuilder(Object... querySourceBeans) {
        this();
        this.withSingletonQuerySources(querySourceBeans);
    }

    public GraphQLSchemaBuilder withDefaults() {
        return withDefaultMappers()
                .withDefaultConverters();
    }

    public GraphQLSchemaBuilder withDefaultMappers() {
        return withTypeMappers(new RelayIdMapper(), new ScalarMapper(), new EnumMapper(), new MapToListTypeAdapter<>(),
                new VoidToBooleanTypeAdapter(), new ListMapper(), new PageMapper(), new ObjectTypeMapper());
    }

    public GraphQLSchemaBuilder withDefaultConverters() {
        return withInputConverters(new MapToListTypeAdapter<>())
                .withOutputConverters(new MapToListTypeAdapter<>(), new VoidToBooleanTypeAdapter(), new CollectionToListOutputConverter());
    }

    public GraphQLSchemaBuilder withSingletonQuerySources(Object... querySourceBeans) {
        Arrays.stream(querySourceBeans).forEach(this::withSingletonQuerySource);
        return this;
    }

    public GraphQLSchemaBuilder withDomainQuerySources(Type... domainQuerySources) {
        Arrays.stream(domainQuerySources).forEach(this::withDomainQuerySource);
        return this;
    }

    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean) {
        return withSingletonQuerySource(querySourceBean, querySourceBean.getClass());
    }

    public GraphQLSchemaBuilder withDomainQuerySource(Type domainQuerySource) {
        return withDomainQuerySource(GenericTypeReflector.annotate(domainQuerySource));
    }

    public GraphQLSchemaBuilder withDomainQuerySource(AnnotatedType domainQuerySource) {
        this.querySourceRepository.registerDomainQuerySource(domainQuerySource);
        return this;
    }

    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, Type beanType) {
        return withSingletonQuerySource(querySourceBean, GenericTypeReflector.annotate(beanType));
    }

    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, AnnotatedType beanType) {
        this.querySourceRepository.registerSingletonQuerySource(querySourceBean, beanType);
        return this;
    }

    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, Type beanType, ResolverExtractor... extractors) {
        return withSingletonQuerySource(querySourceBean, GenericTypeReflector.annotate(beanType), extractors);
    }

    public GraphQLSchemaBuilder withSingletonQuerySource(Object querySourceBean, AnnotatedType beanType, ResolverExtractor... extractors) {
        this.querySourceRepository.registerSingletonQuerySource(querySourceBean, beanType, Arrays.asList(extractors));
        return this;
    }

    public GraphQLSchemaBuilder withDomainQuerySource(Type domainQuerySource, ResolverExtractor... extractors) {
        return withDomainQuerySource(GenericTypeReflector.annotate(domainQuerySource), extractors);
    }

    public GraphQLSchemaBuilder withDomainQuerySource(AnnotatedType domainQuerySource, ResolverExtractor... extractors) {
        this.querySourceRepository.registerDomainQuerySource(domainQuerySource, Arrays.asList(extractors));
        return this;
    }

    public GraphQLSchemaBuilder withSchemaProcessors(GraphQLSchemaProcessor... processors) {
        addAll(this.processors, processors);
        return this;
    }

    public GraphQLSchemaBuilder withResolverExtractors(ResolverExtractor... resolverExtractors) {
        this.querySourceRepository.registerGlobalQueryExtractors(resolverExtractors);
        return this;
    }

    public GraphQLSchemaBuilder withInputConverters(InputConverter<?,?>... inputConverters) {
        this.converterRepository.registerConverters(inputConverters);
        return this;
    }

    public GraphQLSchemaBuilder withOutputConverters(OutputConverter<?,?>... outputConverters) {
        this.converterRepository.registerConverters(outputConverters);
        return this;
    }

    public GraphQLSchemaBuilder withTypeMappers(TypeMapper... typeMappers) {
        this.typeMappers.registerTypeMappers(typeMappers);
        return this;
    }

    public GraphQLSchemaBuilder withTypeAdapters(AbstractTypeAdapter<?,?>... typeAdapters) {
        withInputConverters((InputConverter<?, ?>[]) typeAdapters);
        withOutputConverters((OutputConverter<?, ?>[]) typeAdapters);
        return withTypeMappers((TypeMapper[]) typeAdapters);
    }

    private void init() {
        if (typeMappers.isEmpty()) {
            withDefaultMappers();
        }
        if (converterRepository.isEmpty()) {
            withDefaultConverters();
        }
    }

    public GraphQLSchema build() {
        init();

        QueryGenerator queryGenerator = new QueryGenerator(querySourceRepository, typeMappers, converterRepository, BuildContext.TypeGenerationMode.FLAT);

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
