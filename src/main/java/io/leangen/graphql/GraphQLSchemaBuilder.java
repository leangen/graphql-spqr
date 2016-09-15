package io.leangen.graphql;

import graphql.schema.GraphQLSchema;
import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.QuerySourceRepository;
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
 * Created by bojan.tomic on 3/2/16.
 */
public class GraphQLSchemaBuilder {

    private final QuerySourceRepository querySourceRepository = new QuerySourceRepository();
    private final Collection<GraphQLSchemaProcessor> processors = new HashSet<>();

    public GraphQLSchemaBuilder() {
        withResolverExtractors(new AnnotatedResolverExtractor());
    }

    public GraphQLSchemaBuilder(Object... querySourceBeans) {
        this();
        this.withSingletonQuerySources(querySourceBeans);
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

    public GraphQLSchema build() {

        QueryGenerator queryGenerator = new QueryGenerator(querySourceRepository, BuildContext.TypeGenerationMode.FLAT);

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
