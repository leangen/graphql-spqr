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

import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.query.OperationBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;

public class OperationRepository {

    private final Set<Operation> rootQueries;
    private final Set<Operation> mutations;
    private final OperationSourceRepository operationSourceRepository;
    private final OperationBuilder operationBuilder;

    public OperationRepository(OperationSourceRepository operationSourceRepository, OperationBuilder operationBuilder) {
        this.operationSourceRepository = operationSourceRepository;
        this.operationBuilder = operationBuilder;
        Collection<Resolver> resolvers = buildQueryResolvers(operationSourceRepository.getOperationSources());
        Collection<Resolver> mutationResolvers = buildMutationResolvers(operationSourceRepository.getOperationSources());
        rootQueries = buildQueries(resolvers);
        mutations = buildMutations(mutationResolvers);
    }

    private Set<Operation> buildQueries(Collection<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).entrySet().stream()
                .map(entry -> operationBuilder.buildQuery(entry.getValue()))
                .collect(Collectors.toSet());
    }

    private Set<Operation> buildMutations(Collection<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).entrySet().stream()
                .map(entry -> operationBuilder.buildMutation(entry.getValue()))
                .collect(Collectors.toSet());
    }

    public Collection<Operation> getQueries() {
        return rootQueries;
    }

    public Collection<Operation> getMutations() {
        return mutations;
    }

    public Set<Operation> getNestedQueries(AnnotatedType domainType) {
        OperationSource domainSource = operationSourceRepository.nestedSourceForType(domainType);
        return buildNestedQueries(domainSource);
    }

    public Collection<Operation> getChildQueries(AnnotatedType domainType) {
        Map<String, Operation> children = new HashMap<>();

        Map<String, Operation> nestedQueries = getNestedQueries(domainType).stream().collect(Collectors.toMap(Operation::getName, Function.identity()));
        /*TODO check if any nested query has a @GraphQLContext field of type different then domainType.
        If so, throw an error early, as such an operation will be impossible to invoke, unless they're static!
        Not sure about @RootContext*/
        Map<String, Operation> embeddableQueries = getEmbeddableQueries(domainType.getType()).stream().collect(Collectors.toMap(Operation::getName, Function.identity()));
        children.putAll(nestedQueries);
        children.putAll(embeddableQueries);
        return children.values();
    }

    public Set<Operation> getEmbeddableQueries(Type domainType) {
        return getQueries().stream()
                .filter(query -> query.isEmbeddableForType(domainType))
                .collect(Collectors.toSet());
    }

    private Set<Operation> buildNestedQueries(OperationSource operationSource) {
        return buildQueries(buildQueryResolvers(Collections.singleton(operationSource)));
    }

    private Collection<Resolver> buildQueryResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildQueryResolvers(operationSource.getServiceSingleton(), operationSource.getJavaType())));
    }

    private Collection<Resolver> buildMutationResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildMutationResolvers(operationSource.getServiceSingleton(), operationSource.getJavaType())));
    }

    private Collection<Resolver> buildResolvers(Collection<OperationSource> operationSources, BiFunction<OperationSource, ResolverBuilder, Collection<Resolver>> building) {
        Collection<Resolver> resolvers = new HashSet<>();
        operationSources.forEach(
                operationSource -> operationSource.getResolverBuilders().forEach(
                        builder -> resolvers.addAll(building.apply(operationSource, builder)))
        );
        return resolvers;
    }
}
