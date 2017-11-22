package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final Set<Operation> queries;
    private final Set<Operation> mutations;
    private final Set<Operation> subscriptions;
    private final OperationSourceRepository operationSourceRepository;
    private final OperationBuilder operationBuilder;

    public OperationRepository(OperationSourceRepository operationSourceRepository, OperationBuilder operationBuilder) {
        this.operationSourceRepository = operationSourceRepository;
        this.operationBuilder = operationBuilder;
        List<Resolver> resolvers = buildQueryResolvers(operationSourceRepository.getOperationSources());
        List<Resolver> mutationResolvers = buildMutationResolvers(operationSourceRepository.getOperationSources());
        List<Resolver> subscriptionResolvers = buildSubscriptionResolvers(operationSourceRepository.getOperationSources());
        queries = buildQueries(resolvers);
        mutations = buildMutations(mutationResolvers);
        subscriptions = buildSubscriptions(subscriptionResolvers);
    }

    private Set<Operation> buildQueries(List<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).values().stream()
                .flatMap(r -> collectContextTypes(r).stream()
                        .map(contextType -> resolversPerContext(contextType, r))
                        .filter(contextual -> !contextual.getValue().isEmpty())
                        .map(contextual -> operationBuilder.buildQuery(contextual.getKey(), contextual.getValue())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Operation> buildMutations(List<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).values().stream()
                .flatMap(r -> collectContextTypes(r).stream()
                        .map(contextType -> resolversPerContext(contextType, r))
                        .filter(contextual -> !contextual.getValue().isEmpty())
                        .map(contextual -> operationBuilder.buildMutation(contextual.getKey(), contextual.getValue())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Operation> buildSubscriptions(List<Resolver> resolvers) {
        return resolvers.stream()
                .collect(Collectors.groupingBy(Resolver::getOperationName)).values().stream()
                .flatMap(r -> collectContextTypes(r).stream()
                        .map(contextType -> resolversPerContext(contextType, r))
                        .filter(contextual -> !contextual.getValue().isEmpty())
                        .map(contextual -> operationBuilder.buildSubscription(contextual.getKey(), contextual.getValue())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map.Entry<Type, List<Resolver>> resolversPerContext(Type context, List<Resolver> resolvers) {
        List<Resolver> contextual;
        if (context == null) {
            contextual = resolvers.stream().filter(r -> r.getSourceTypes().isEmpty()).collect(Collectors.toList());
        } else {
            contextual = resolvers.stream().filter(r -> r.getSourceTypes().contains(context)).collect(Collectors.toList());
        }
        return new AbstractMap.SimpleEntry<>(context, contextual);
    }

    private List<Type> collectContextTypes(Collection<Resolver> resolvers) {
        List<Type> contextTypes = resolvers.stream()
                .flatMap(r -> r.getSourceTypes().stream())
                .distinct()
                .collect(Collectors.toList());
        contextTypes.add(null); //root queries have null context
        return contextTypes;
    }

    private Collection<Operation> getAllQueries() {
        return queries;
    }

    public Collection<Operation> getRootQueries() {
        return queries.stream().filter(Operation::isRoot).collect(Collectors.toList());
    }

    public Collection<Operation> getMutations() {
        return mutations;
    }

    public Collection<Operation> getSubscriptions() {
        return subscriptions;
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
        return getAllQueries().stream()
                .map(Operation::unbatch)
                .filter(query -> query.isEmbeddableForType(domainType))
                .collect(Collectors.toSet());
    }

    private Set<Operation> buildNestedQueries(OperationSource operationSource) {
        return buildQueries(buildQueryResolvers(Collections.singleton(operationSource)));
    }

    private List<Resolver> buildQueryResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildQueryResolvers(operationSource.getServiceSingleton(), operationSource.getJavaType())));
    }

    private List<Resolver> buildMutationResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildMutationResolvers(operationSource.getServiceSingleton(), operationSource.getJavaType())));
    }

    private List<Resolver> buildSubscriptionResolvers(Collection<OperationSource> operationSources) {
        return buildResolvers(operationSources, ((operationSource, builder) ->
                builder.buildSubscriptionResolvers(operationSource.getServiceSingleton(), operationSource.getJavaType())));
    }

    private List<Resolver> buildResolvers(Collection<OperationSource> operationSources, BiFunction<OperationSource, ResolverBuilder, Collection<Resolver>> building) {
        return operationSources.stream()
                .flatMap(operationSource ->
                        operationSource.getResolverBuilders().stream()
                                .flatMap(builder -> building.apply(operationSource, builder).stream())
                                .distinct())
                .collect(Collectors.toList());
    }
}
