package io.leangen.graphql.metadata;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.query.ConnectionRequest;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by bojan.tomic on 3/19/16.
 */
public class Query {

    public static final Query ROOT_QUERY = new Query();

    private String name;
    private String description;
    private Query parent;
    private boolean virtual;
    private AnnotatedType javaType;
    private Type sourceType;
    private Map<String, QueryResolver> resolversByFingerprint;
    private List<QueryArgument> arguments;
    private List<QueryArgument> sortableArguments;

    private boolean root;
    private boolean hasPrimaryResolver;
    private String hierarchicalName;

    private Query() {
        this.name = GraphQLQuery.ROOT_QUERY_ALIAS;
        this.hierarchicalName = this.name;
    }

    public Query(String name, List<QueryResolver> resolvers, Query parent) {
        this.name = name;
        this.description = resolvers.stream().map(QueryResolver::getQueryDescription).filter(desc -> !desc.isEmpty()).findFirst().orElse("");
        this.parent = parent;
        this.root = parent.equals(ROOT_QUERY);
        this.hierarchicalName = resolveHierarchicalName(parent) + "." + name;
        this.virtual = resolvers.isEmpty();
        this.hasPrimaryResolver = resolvers.stream().filter(QueryResolver::isPrimaryResolver).findFirst().isPresent();
        this.javaType = virtual ? null : resolveJavaType(resolvers, name);
        this.sourceType = virtual ? null : resolveSourceType(resolvers);
        this.resolversByFingerprint = collectResolversByFingerprint(hierarchicalName, resolvers);
        this.arguments = virtual ? Collections.emptyList() : collectArguments(resolvers);
        this.sortableArguments = virtual ? Collections.emptyList() : collectArguments(
                resolvers.stream().filter(QueryResolver::supportsConnectionRequests).collect(Collectors.toList()));
    }

    private AnnotatedType resolveJavaType(List<QueryResolver> resolvers, String queryName) {
        if (resolvers.size() == 1) {
            return resolvers.get(0).getReturnType();
        }
        List<AnnotatedType> returnTypes = resolvers.stream()
                .map(QueryResolver::getReturnType)
                .collect(Collectors.toList());
        AnnotatedType mostSpecificSuperType = ClassUtils.getCommonSuperType(returnTypes);
        if (mostSpecificSuperType.getType() == Object.class || mostSpecificSuperType.getType() == Cloneable.class || mostSpecificSuperType.getType() == Serializable.class) {
            throw new IllegalArgumentException("Resolvers for query " + queryName + " do not return compatible types");
        }
        Annotation[] aggregatedAnnotations = resolvers.stream()
                .flatMap(resolver -> Arrays.stream(resolver.getReturnType().getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
        return GenericTypeReflector.replaceAnnotations(mostSpecificSuperType, aggregatedAnnotations);
    }

    private Type resolveSourceType(List<QueryResolver> resolvers) {
        List<Type> sourceTypes = resolvers.stream()
                .map(QueryResolver::getSourceType)
                .distinct()
                .collect(Collectors.toList());
        if (sourceTypes.size() > 1) {
            throw new IllegalStateException("Not all resolvers expect the same source type");
        }
        return sourceTypes.get(0);
    }

    private Map<String, QueryResolver> collectResolversByFingerprint(String hierarchicalName, List<QueryResolver> resolvers) {
        Map<String, QueryResolver> resolversByFingerprint = new HashMap<>();
        resolvers.forEach(resolver -> resolver.getFingerprints(hierarchicalName).forEach(fingerprint -> resolversByFingerprint.put(fingerprint, resolver)));
        return resolversByFingerprint;
    }

    private Type resolveGenericType(String queryName, List<QueryResolver> resolvers) {
        Map<Type, String> knownTypes = new HashMap<>();
        resolvers.forEach(resolver -> knownTypes.putIfAbsent(resolver.getReturnType().getType(), resolver.toString()));

        if (knownTypes.size() != 1) {
            StringBuilder message = new StringBuilder("Not all resolver methods for query '" + queryName + "' return or wrap with the same type:\n");
            knownTypes.values().forEach(example -> message.append(example).append("\n"));
            throw new IllegalArgumentException(message.toString());
        }

        return knownTypes.keySet().iterator().next();
    }

    //TODO do annotations or overloading decide what arg is required? should that decision be externalized?
    private List<QueryArgument> collectArguments(List<QueryResolver> resolvers) {
        Map<String, List<QueryArgument>> argumentsByName = resolvers.stream()
                .flatMap(resolver -> resolver.getQueryArguments().stream()) // merge all known args for this query
                .collect(Collectors.groupingBy(QueryArgument::getName));

        return argumentsByName.keySet().stream()
                .map(argName -> new QueryArgument(
                        ClassUtils.getCommonSuperType(argumentsByName.get(argName).stream().map(QueryArgument::getJavaType).collect(Collectors.toList())),
                        argName,
                        "",
//						argumentsByName.get(argName).size() == resolvers.size() || argumentsByName.get(argName).stream().anyMatch(QueryArgument::isRequired),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isRequired),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isResolverSource),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isContext),
                        argumentsByName.get(argName).stream().anyMatch(QueryArgument::isRelayConnection)
                ))
                .collect(Collectors.toList());
    }

    public Object resolve(DataFetchingEnvironment env, ExecutionContext executionContext) {
        Map<String, Object> queryArguments = new HashMap<>();
        Map<String, Object> connectionArguments = new HashMap<>();

        env.getArguments().entrySet().forEach(arg -> {
            if (arg.getValue() != null) {
                if (ConnectionRequest.isConnectionArgumentName(arg.getKey())) {
                    connectionArguments.put(arg.getKey(), arg.getValue());
                } else {
                    queryArguments.put(arg.getKey(), arg.getValue());
                }
            }
        });

        QueryResolver resolver = resolversByFingerprint.get(getFingerprint(queryArguments));
        try {
            if (resolver == null) {
                if (queryArguments.size() == 0 && !root) {
                    return ClassUtils.getFieldValue(env.getSource(), name);
                } else {
                    //TODO implement simple filtering here
                }
            } else {
                Object result = resolver.resolve(env.getSource(), queryArguments, new ConnectionRequest(connectionArguments), executionContext);
                return executionContext.proxyIfNeeded(env, result, resolver.getReturnType().getType());
            }
            throw new GraphQLException("Resolver for query " + hierarchicalName + " accepting arguments: " + env.getArguments().keySet() + " not implemented");
        } catch (Exception e) {
            throw new GraphQLException("Query resolution exception", e);
        }
    }

    public boolean isEmbeddableForType(Type type) {
        return this.parent == ROOT_QUERY && this.sourceType != null && ClassUtils.isSuperType(this.sourceType, type);
    }

    private String resolveHierarchicalName(Query query) {
        if (query.getParent() == null) {
            return query.name;
        }
        return resolveHierarchicalName(query.getParent()) + "." + query.name;
    }

    private String getFingerprint(Map<String, Object> arguments) {
        StringBuilder fingerPrint = new StringBuilder(hierarchicalName);
        arguments.keySet().stream().sorted().forEach(fingerPrint::append);
        return fingerPrint.toString();
    }

    public int getFingerprint() {
        StringBuilder fingerprint = new StringBuilder();
        resolversByFingerprint.values().forEach(resolver -> fingerprint.append(resolver.getFingerprint()));
        return fingerprint.toString().hashCode();
    }

    public boolean isPageable() {
        return Page.class.isAssignableFrom(ClassUtils.getRawType(javaType.getType()));
    }

    public boolean hasPrimaryResolver() {
        return hasPrimaryResolver;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    public Query getParent() {
        return parent;
    }

    public List<QueryArgument> getArguments() {
        return arguments;
    }

    public boolean isVirtual() {
        return virtual;
    }

    @Override
    public int hashCode() {
        return (name + javaType.toString() + (parent == null ? "" : parent.name)).hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Query)) return false;
        Query otherQuery = (Query) other;

        if ((otherQuery.parent == null && this.parent != null) || (otherQuery.parent != null && this.parent == null)) {
            return false;
        }

        return otherQuery.name.equals(this.name) &&
                ((otherQuery.javaType == null && this.javaType == null) || (otherQuery.javaType.equals(this.javaType))) &&
                ((otherQuery.parent == null && this.parent == null) || otherQuery.parent.equals(this.parent));
    }

    @Override
    public String toString() {
        return hierarchicalName;
    }

}
