package io.leangen.graphql.generator;

import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.metadata.strategy.query.ResolverExtractor;
import io.leangen.graphql.query.conversion.ConverterRepository;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.leangen.graphql.metadata.Query.ROOT_QUERY;

/**
 * Created by bojan.tomic on 3/17/16.
 */
public class QueryRepository {

    private QueryTreeNode queries;
    private Set<Query> mutations;
    private QuerySourceRepository querySourceRepository;
    private TypeMapperRepository typeMappers;
    private ConverterRepository converters;

    public QueryRepository(QuerySourceRepository querySourceRepository, TypeMapperRepository typeMappers, ConverterRepository converters) {
        this.querySourceRepository = querySourceRepository;
        this.typeMappers = typeMappers;
        this.converters = converters;
        Collection<QueryResolver> queryResolvers = extractQueryResolvers(querySourceRepository.getQuerySources(), typeMappers, converters);
        Collection<QueryResolver> mutationResolvers = extractMutationResolvers(querySourceRepository.getQuerySources(), typeMappers, converters);
        queries = registerQueries(ROOT_QUERY, queryResolvers.stream().map(ModifiableQueryResolver::new).collect(Collectors.toList()));
        mutations = mutationResolvers.stream()
                .collect(Collectors.groupingBy(QueryResolver::getQueryName)).entrySet().stream()
                .map(entry -> new Query(entry.getKey(), entry.getValue(), ROOT_QUERY))
                .collect(Collectors.toSet());
    }

    private QueryTreeNode registerQueries(Query query, Collection<ModifiableQueryResolver> resolvers) {
        QueryTreeNode node = new QueryTreeNode(query);

        resolvers.stream()
                .filter(wrappedResolver -> wrappedResolver.isChildResolverOf(node.query))
                .map(wrappedResolver -> wrappedResolver.resolver)
                .collect(Collectors.groupingBy(QueryResolver::getQueryName))
                .entrySet().stream()
                .map(entry -> new Query(entry.getKey(), entry.getValue(), node.query))
                .forEach(child -> node.addChild(registerQueries(child, resolvers)));


        resolvers.stream()
                .map(wrappedResolver -> wrappedResolver.shiftParent(node.name))
                .distinct()
                .filter(queryName -> null != queryName)
                .map(queryName -> new Query(queryName, Collections.emptyList(), node.query))
                .forEach(child -> node.addChild(registerQueries(child, resolvers)));

        return node;
    }

    public Collection<Query> getRootQueries() {
        return queries.children.values().stream().map(node -> node.query).collect(Collectors.toList());
    }

    public Collection<Query> getMutations() {
        return mutations;
    }

    public boolean contains(List<String> trail, String queryName) {
        try {
            return get(new LinkedList<>(trail), queryName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Query get(List<String> trail, String queryName) {
        LinkedList<String> fullTrail = new LinkedList<>(trail);
        fullTrail.add(queryName);
        return getNode(fullTrail, queries).query;
    }

    public Set<Query> getDomainQueries(List<String> trail, AnnotatedType domainType) {
        QuerySource domainSource = querySourceRepository.domainSourceForType(domainType);
        return assembleDomainQueries(trail, domainSource);
    }

    public Collection<Query> getChildQueries(List<String> trail, AnnotatedType domainType) {
        QueryTreeNode node = findNode(trail, queries);
        Map<String, Query> children = new HashMap<>();

        children.putAll(getDomainQueries(trail, domainType).stream().collect(Collectors.toMap(Query::getName, Function.identity())));
        children.putAll(getEmbeddableQueries(domainType.getType()).stream().collect(Collectors.toMap(Query::getName, Function.identity())));
        if (node != null) {
            children.putAll(node.children.values().stream().map(wrapper -> wrapper.query).collect(Collectors.toMap(Query::getName, Function.identity())));
        }
        return children.values();
    }

    public Set<Query> getEmbeddableQueries(Type domainType) {
        return getRootQueries().stream()
                .filter(query -> query.isEmbeddableForType(domainType))
                .collect(Collectors.toSet());
    }

    private QueryTreeNode findNode(List<String> trail, QueryTreeNode current) {
        return getNode(new LinkedList<>(trail), current);
    }

    private QueryTreeNode getNode(Queue<String> trail, QueryTreeNode current) {
        if (trail.isEmpty()) {
            return current;
        }
        String queryName = trail.remove();
        return current.get(queryName) == null ? null : getNode(trail, current.get(queryName));
    }

    private Set<Query> assembleDomainQueries(List<String> trail, QuerySource querySource) {
        QueryTreeNode node = findNode(trail, queries);
        return extractQueryResolvers(Collections.singleton(querySource), typeMappers, converters).stream()
                .collect(Collectors.groupingBy(QueryResolver::getQueryName)).entrySet().stream()
                .map(entry -> new Query(entry.getKey(), entry.getValue(), node == null ? ROOT_QUERY :node.query))
                .collect(Collectors.toSet());
    }

    private Collection<QueryResolver> extractQueryResolvers(Collection<QuerySource> querySources, TypeMapperRepository typeMappers, ConverterRepository converters) {
        return extractResolvers(querySources, ((querySource, extractor) ->
                extractor.extractQueryResolvers(querySource.getQuerySourceBean(), querySource.getJavaType(), typeMappers, converters)));
    }

    private Collection<QueryResolver> extractMutationResolvers(Collection<QuerySource> querySources, TypeMapperRepository typeMappers, ConverterRepository converters) {
        return extractResolvers(querySources, ((querySource, extractor) ->
                extractor.extractMutationResolvers(querySource.getQuerySourceBean(), querySource.getJavaType(), typeMappers, converters)));
    }

    private Collection<QueryResolver> extractResolvers(Collection<QuerySource> querySources, BiFunction<QuerySource, ResolverExtractor, Collection<QueryResolver>> extraction) {
        Collection<QueryResolver> queryResolvers = new HashSet<>();
        querySources.forEach(
                querySource -> querySource.getExtractors().forEach(
                        extractor -> queryResolvers.addAll(extraction.apply(querySource, extractor)))
        );
        return queryResolvers;
    }

    private static class QueryTreeNode {
        private String name;
        private Query query;
        private Map<String, QueryTreeNode> children = new HashMap<>();

        QueryTreeNode(Query query) {
            this.name = query.getName();
            this.query = query;
        }

        void addChild(QueryTreeNode child) {
            children.putIfAbsent(child.name, child);
        }

        QueryTreeNode get(String name) {
            return children.get(name);
        }
    }

    private static class ModifiableQueryResolver {
        private QueryResolver resolver;
        private Set<LinkedList<String>> parentTrails;

        ModifiableQueryResolver(QueryResolver resolver) {
            this.resolver = resolver;
            this.parentTrails = resolver.getParentQueryTrails().stream()
                    .map(LinkedList::new)
                    .collect(Collectors.toSet());
        }

        boolean isChildResolverOf(Query query) {
            return resolver.getParentQueryTrails().stream().anyMatch(parentTrail -> isChildResolverOf(query, parentTrail, parentTrail.size() - 1));
        }

        boolean isChildResolverOf(Query query, List<String> parentTrail, int index) {
            if (index == -1) {
                return true;
            }
            if (!parentTrail.get(index).equals(query.getName())) return false;
            return isChildResolverOf(query.getParent(), parentTrail, --index);
        }

        String shiftParent(String queryName) {
            for (LinkedList<String> parentTrail : parentTrails) {
                if (queryName.equals(parentTrail.getFirst()) && !queryName.equals(parentTrail.getLast())) {
                    parentTrail.remove();
                    return parentTrail.peek();
                }
            }
            return null;
        }
    }
}
