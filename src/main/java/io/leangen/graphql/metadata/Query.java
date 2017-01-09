package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;
import io.leangen.graphql.query.ConnectionRequest;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;

import static java.util.Arrays.stream;

public class Query {

    private final String name;
    private final String description;
    private final AnnotatedType javaType;
    private final List<Type> sourceTypes;
    private final Map<String, QueryResolver> resolversByFingerprint;
    private final List<QueryArgument> arguments;
    private final List<QueryArgument> sortableArguments;

    private boolean hasPrimaryResolver;

    public Query(String name, AnnotatedType javaType, List<Type> sourceTypes, List<QueryArgument> arguments, 
                 List<QueryArgument> sortableArguments, List<QueryResolver> resolvers) {
        
        this.name = name;
        this.description = resolvers.stream().map(QueryResolver::getQueryDescription).filter(desc -> !desc.isEmpty()).findFirst().orElse("");
        this.hasPrimaryResolver = resolvers.stream().anyMatch(QueryResolver::isPrimaryResolver);
        this.javaType = javaType;
        this.sourceTypes = sourceTypes;
        this.resolversByFingerprint = collectResolversByFingerprint(resolvers);
        this.arguments = arguments;
        this.sortableArguments = sortableArguments;
    }

    private Map<String, QueryResolver> collectResolversByFingerprint(List<QueryResolver> resolvers) {
        Map<String, QueryResolver> resolversByFingerprint = new HashMap<>();
        resolvers.forEach(resolver -> resolver.getFingerprints().forEach(fingerprint -> resolversByFingerprint.put(fingerprint, resolver)));
        return resolversByFingerprint;
    }

    public Object resolve(DataFetchingEnvironment env, InputDeserializer inputDeserializer, ExecutionContext executionContext) {
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
                Object result = resolver.resolve(env.getSource(), env.getContext(), queryArguments, inputDeserializer, new ConnectionRequest(connectionArguments), executionContext);
                return result;
            }
            throw new GraphQLException("Resolver for query " + name + " accepting arguments: " + env.getArguments().keySet() + " not implemented");
        } catch (Exception e) {
            throw new GraphQLException("Query resolution exception", e);
        }
    }

    public boolean isEmbeddableForType(Type type) {
        return this.sourceTypes.stream().anyMatch(sourceType -> GenericTypeReflector.isSuperType(sourceType, type));
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
        int typeHash = stream(javaType.getAnnotations())
                .mapToInt(annotation -> annotation.getClass().getName().hashCode())
                .reduce((x, y) -> x ^ y).orElse(0);
        return name.hashCode() ^ typeHash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Query)) return false;
        Query that = (Query) other;

        if ((that.javaType == null && this.javaType != null) || (that.javaType != null && this.javaType == null)) {
            return false;
        }

        return that.name.equals(this.name)
                && (that.javaType == null || that.javaType.equals(this.javaType));
    }

    @Override
    public String toString() {
        return name + "(" + String.join(",", arguments.stream().map(QueryArgument::getName).collect(Collectors.toList())) + ")";
    }
}
