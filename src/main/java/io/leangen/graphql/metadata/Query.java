package io.leangen.graphql.metadata;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.query.ConnectionRequest;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 3/19/16.
 */
public class Query {

    private String name;
    private String description;
    private AnnotatedType javaType;
    private Type sourceType;
    private Map<String, QueryResolver> resolversByFingerprint;
    private List<QueryArgument> arguments;
    private List<QueryArgument> sortableArguments;

    private boolean hasPrimaryResolver;

    public Query(String name, List<QueryResolver> resolvers) {
        this.name = name;
        this.description = resolvers.stream().map(QueryResolver::getQueryDescription).filter(desc -> !desc.isEmpty()).findFirst().orElse("");
        this.hasPrimaryResolver = resolvers.stream().filter(QueryResolver::isPrimaryResolver).findFirst().isPresent();
        this.javaType = resolveJavaType(resolvers, name);
        this.sourceType = resolveSourceType(resolvers);
        this.resolversByFingerprint = collectResolversByFingerprint(resolvers);
        this.arguments = collectArguments(resolvers);
        this.sortableArguments = collectArguments(
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

    private Map<String, QueryResolver> collectResolversByFingerprint(List<QueryResolver> resolvers) {
        Map<String, QueryResolver> resolversByFingerprint = new HashMap<>();
        resolvers.forEach(resolver -> resolver.getFingerprints().forEach(fingerprint -> resolversByFingerprint.put(fingerprint, resolver)));
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
                        argumentsByName.get(argName).stream().map(QueryArgument::getDescription).filter(desc -> desc != null).findFirst().orElse(""),
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
                if (queryArguments.size() == 0 && env.getSource() != null) {
                    return ClassUtils.getFieldValue(env.getSource(), name);
                } else {
                    //TODO implement simple filtering here
                }
            } else {
                Object result = resolver.resolve(env.getSource(), env.getContext(), queryArguments, new ConnectionRequest(connectionArguments), executionContext);
                return executionContext.proxyIfNeeded(env, result, resolver.getReturnType().getType());
            }
            throw new GraphQLException("Resolver for query " + name + " accepting arguments: " + env.getArguments().keySet() + " not implemented");
        } catch (Exception e) {
            throw new GraphQLException("Query resolution exception", e);
        }
    }

    public boolean isEmbeddableForType(Type type) {
        return this.sourceType != null && ClassUtils.isSuperType(this.sourceType, type);
    }

    private String getFingerprint(Map<String, Object> arguments) {
        StringBuilder fingerPrint = new StringBuilder();
        arguments.keySet().stream().sorted().forEach(fingerPrint::append);
        return fingerPrint.toString();
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

    public List<QueryArgument> getArguments() {
        return arguments;
    }

    public Collection<QueryResolver> getResolvers() {
        return resolversByFingerprint.values();
    }

    @Override
    public int hashCode() {
        int typeHash = Arrays.stream(javaType.getAnnotations())
                .mapToInt(annotation -> annotation.getClass().getCanonicalName().hashCode())
                .sum();
        return name.hashCode() + typeHash;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Query)) return false;
        Query otherQuery = (Query) other;

        if ((otherQuery.javaType == null && this.javaType != null) || (otherQuery.javaType != null && this.javaType == null)) {
            return false;
        }

        return otherQuery.name.equals(this.name)
                && (otherQuery.javaType == null || otherQuery.javaType.equals(this.javaType));
    }

    @Override
    public String toString() {
        return name + "(" + String.join(",", arguments.stream().map(QueryArgument::getName).collect(Collectors.toList())) + ")";
    }
}
