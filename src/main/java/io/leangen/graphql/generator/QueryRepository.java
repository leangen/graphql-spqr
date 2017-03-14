package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.metadata.strategy.query.QueryBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverExtractor;

public class QueryRepository {

    private final Set<Query> rootQueries;
    private final Set<Query> mutations;
    private final QuerySourceRepository querySourceRepository;
    private final QueryBuilder queryBuilder;

    public QueryRepository(QuerySourceRepository querySourceRepository, QueryBuilder queryBuilder) {
        this.querySourceRepository = querySourceRepository;
        this.queryBuilder = queryBuilder;
        Collection<QueryResolver> queryResolvers = extractQueryResolvers(querySourceRepository.getQuerySources());
        Collection<QueryResolver> mutationResolvers = extractMutationResolvers(querySourceRepository.getQuerySources());
        rootQueries = assembleQueries(queryResolvers);
        mutations = assembleMutations(mutationResolvers);
    }

    private Set<Query> assembleQueries(Collection<QueryResolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(QueryResolver::getQueryName)).entrySet().stream()
                .map(entry -> queryBuilder.buildQuery(entry.getValue()))
                .collect(Collectors.toSet());
    }

    private Set<Query> assembleMutations(Collection<QueryResolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(QueryResolver::getQueryName)).entrySet().stream()
                .map(entry -> queryBuilder.buildMutation(entry.getValue()))
                .collect(Collectors.toSet());
    }

    public Collection<Query> getQueries() {
        return rootQueries;
    }

    public Collection<Query> getMutations() {
        return mutations;
    }

    public Set<Query> getDomainQueries(AnnotatedType domainType) {
        QuerySource domainSource = querySourceRepository.domainSourceForType(domainType);
        return assembleDomainQueries(domainSource);
    }

    public Collection<Query> getChildQueries(AnnotatedType domainType) {
        Map<String, Query> children = new HashMap<>();

        Map<String, Query> domainQueries = getDomainQueries(domainType).stream().collect(Collectors.toMap(Query::getName, Function.identity()));
        Map<String, Query> embeddableQueries = getEmbeddableQueries(domainType.getType()).stream().collect(Collectors.toMap(Query::getName, Function.identity()));
        children.putAll(domainQueries);
        children.putAll(embeddableQueries);
        return children.values();
    }

    public Set<Query> getEmbeddableQueries(Type domainType) {
        return getQueries().stream()
                .filter(query -> query.isEmbeddableForType(domainType))
                .collect(Collectors.toSet());
    }

    private Set<Query> assembleDomainQueries(QuerySource querySource) {
        return assembleQueries(extractQueryResolvers(Collections.singleton(querySource)));
    }

    private Collection<QueryResolver> extractQueryResolvers(Collection<QuerySource> querySources) {
        return extractResolvers(querySources, ((querySource, extractor) ->
                extractor.extractQueryResolvers(querySource.getQuerySourceBean(), querySource.getJavaType())));
    }

    private Collection<QueryResolver> extractMutationResolvers(Collection<QuerySource> querySources) {
        return extractResolvers(querySources, ((querySource, extractor) ->
                extractor.extractMutationResolvers(querySource.getQuerySourceBean(), querySource.getJavaType())));
    }

    private Collection<QueryResolver> extractResolvers(Collection<QuerySource> querySources, BiFunction<QuerySource, ResolverExtractor, Collection<QueryResolver>> extraction) {
        Collection<QueryResolver> queryResolvers = new HashSet<>();
        querySources.forEach(
                querySource -> querySource.getExtractors().forEach(
                        extractor -> queryResolvers.addAll(extraction.apply(querySource, extractor)))
        );
        return queryResolvers;
    }
}
