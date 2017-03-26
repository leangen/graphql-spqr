package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.graphql.annotations.RelayConnectionRequest;
import io.leangen.graphql.execution.ConnectionRequest;
import io.leangen.graphql.metadata.execution.Executable;
import io.leangen.graphql.util.ClassUtils;

/**
 * Class representing a single method used to resolve a specific query given specific arguments.
 * A single query can have multiple resolvers, corresponding to different combinations of arguments.
 * This is done mainly to support attaching multiple overloaded methods as resolvers for the same query.
 * Two resolvers of the same query must not accept the same list of argument names.
 *
 * @author bojan.tomic (kaqqao)
 */
public class Resolver {

    private final String operationName;
    private final String operationDescription;
    private final List<OperationArgument> arguments;
    private final List<Parameter> connectionRequestArguments;
    private final AnnotatedType returnType;
    private final Set<OperationArgument> sourceArguments;
    private final Executable executable;

    public Resolver(String operationName, String operationDescription, Executable executable, List<OperationArgument> arguments) {
        this.executable = executable;
        this.operationName = operationName;
        this.operationDescription = operationDescription;
        this.arguments = arguments;
        this.connectionRequestArguments = resolveConnectionRequestArguments();
        this.returnType = ClassUtils.stripBounds(executable.getReturnType());
        this.sourceArguments = resolveSources(arguments);
    }

    /**
     * Finds the argument representing the query source (object returned by the parent query), if it exists.
     * Operation source argument will (potentially) exist only for the resolvers of nestable queries.
     * Even then, not all resolvers of such queries necessarily accept a source object.
     *
     * @param arguments All arguments that this resolver accepts
     * @return The arguments representing possible query sources for this resolver
     * (object returned by the parent query), or null if this resolver doesn't accept a query source
     */
    private Set<OperationArgument> resolveSources(List<OperationArgument> arguments) {
        return arguments.stream()
                .filter(OperationArgument::isResolverSource)
                .collect(Collectors.toSet());
    }

    /**
     * Finds all method parameters used for Relay connection-style pagination. Should support arbitrary parameter mapping,
     * but currently only parameters of type {@link ConnectionRequest} or those annotated with {@link RelayConnectionRequest}
     * (but still with no mapping applied) are recognized.
     *
     * @return The parameters used for Relay connection-style pagination
     */
    private List<Parameter> resolveConnectionRequestArguments() {
        List<Parameter> queryContextArguments = new ArrayList<>();
        for (int i = 0; i < executable.getParameterCount(); i++) {
            Parameter parameter = executable.getParameters()[i];
            if (isConnectionRequestArgument(parameter)) {
                queryContextArguments.add(parameter);
            }
        }
        return queryContextArguments;
    }

    private boolean isConnectionRequestArgument(Parameter parameter) {
        return parameter.isAnnotationPresent(RelayConnectionRequest.class) || ConnectionRequest.class.isAssignableFrom(parameter.getType());
    }

    /**
     * Calls the underlying resolver  method/field
     *
     * @param source The object on which the method/field is to be called
     * @param args Arguments to the underlying method (empty if the underlying resolver is a field)
     *
     * @return The result returned by the underlying method/field
     *
     * @throws InvocationTargetException If a reflective invocation of the underlying method/field fails
     * @throws IllegalAccessException If a reflective invocation of the underlying method/field is not allowed
     */
    @SuppressWarnings("unchecked")
    public Object resolve(Object source, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return executable.execute(source, args);
    }

    public boolean supportsConnectionRequests() {
        return !connectionRequestArguments.isEmpty();
    }

    /**
     * Checks whether this resolver is the primary. Primary resolver is the one accepting nothing but the Relay ID.
     *
     * @return Boolean indicating whether this resolver is the primary resolver for this query
     */
    public boolean isPrimaryResolver() {
        return arguments.size() == 1 && arguments.get(0).isRelayId();
    }

    /**
     * Gets the generic Java types of the source objects (object returned by the parent query),
     * if one is accepted by this resolver. Used to decide if this query can be nested inside another.
     *
     * @return The generic Java type of the source object, or null if this resolver does not accept one.
     */
    public Set<Type> getSourceTypes() {
        return sourceArguments.stream().map(arg -> arg.getJavaType().getType()).collect(Collectors.toSet());
    }

    public String getOperationName() {
        return operationName;
    }

    public String getOperationDescription() {
        return operationDescription;
    }

    /**
     * Get the fingerprint of this resolver. Fingerprint uniquely identifies a resolver within a query.
     * It is based on the name of the query and all parameters this specific resolver accepts.
     * It is used to decide which resolver to invoke for the query, based on the provided arguments.
     *
     * @return The unique "fingerprint" string identifying this resolver
     */
    public Set<String> getFingerprints() {
        Set<String> fingerprints = new HashSet<>(sourceArguments.size() + 1);
        sourceArguments.forEach(source -> fingerprints.add(fingerprint(source)));
        fingerprints.add(fingerprint(null));
        return fingerprints;
    }

    public List<OperationArgument> getArguments() {
        return arguments;
    }

    public AnnotatedType getReturnType() {
        return returnType;
    }

    private String fingerprint(OperationArgument ignoredResolverSource) {
        StringBuilder fingerprint = new StringBuilder();
        arguments.stream()
                .filter(arg -> arg != ignoredResolverSource)
                .filter(arg -> !arg.isRelayConnection() && !arg.isContext())
                .map(OperationArgument::getName)
                .sorted()
                .forEach(fingerprint::append);
        return fingerprint.toString();
    }

    @Override
    public boolean equals(Object that) {
        return this == that || that instanceof Resolver && this.executable.equals(((Resolver) that).executable);
    }

    @Override
    public int hashCode() {
        return executable.hashCode();
    }

    @Override
    public String toString() {
        return executable.toString();
    }
}
