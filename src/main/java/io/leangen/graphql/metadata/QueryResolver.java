package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.leangen.graphql.annotations.RelayConnectionRequest;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.query.ConnectionRequest;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.execution.Executable;
import io.leangen.graphql.util.ClassUtils;

/**
 * Class representing a single method used to resolve a specific query given specific arguments.
 * A single query can have multiple resolvers, corresponding to different combinations of arguments.
 * This is done mainly to support attaching multiple overloaded methods as resolvers for the same query.
 * Two resolvers of the same query must not accept the same list of argument names.
 *
 * @author bojan.tomic (kaqqao)
 */
public class QueryResolver {

    private String queryName;
    private String queryDescription;
    private List<QueryArgument> queryArguments;
    private List<Parameter> connectionRequestArguments;
    private AnnotatedType returnType;
    private QueryArgument sourceArgument;
    private String wrappedAttribute;
    private Executable executable;
    private boolean relayId;

    public QueryResolver(String queryName, String queryDescription, boolean relayId, Executable executable, List<QueryArgument> queryArguments) {
        this.executable = executable;
        this.queryName = queryName;
        this.queryDescription = queryDescription;
        this.relayId = relayId;
        this.queryArguments = queryArguments;
        this.connectionRequestArguments = resolveConnectionRequestArguments();
        this.returnType = ClassUtils.stripBounds(executable.getReturnType());
        this.wrappedAttribute = executable.getWrappedAttribute();
        this.sourceArgument = resolveSource(queryArguments);
    }

    /**
     * Finds the argument representing the query source (object returned by the parent query), if it exists.
     * Query source argument will (potentially) exist only for the resolvers of nestable queries.
     * Even then, not all resolvers of such queries necessarily accept a source object.
     *
     * @param arguments All arguments that this resolver accepts
     * @return The argument representing the query source (object returned by the parent query),
     * or null if this resolver doesn't accept a query source
     */
    private QueryArgument resolveSource(List<QueryArgument> arguments) {
        return arguments.stream()
                .filter(QueryArgument::isResolverSource)
                .findFirst()
                .orElse(null);
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
     * Prepares the parameters by mapping/parsing the input and/or source object and invokes the underlying resolver method/field
     *
     * @param source            The source object for this query (the result of the parent query)
     * @param arguments         All regular (non Relay connection specific) arguments as provided in the query
     * @param connectionRequest Relay connection specific arguments provided in the query
     * @param executionContext  An object containing all global information that might be needed during resolver execution
     *
     * @return The result returned by the underlying method/field, potentially proxied and wrapped
     *
     * @throws InvocationTargetException If a reflective invocation of the underlying method/field fails
     * @throws IllegalAccessException If a reflective invocation of the underlying method/field is not allowed
     */
    @SuppressWarnings("unchecked")
    public Object resolve(Object source, Object context, Map<String, Object> arguments, Object connectionRequest, ExecutionContext executionContext) throws InvocationTargetException, IllegalAccessException {
        int queryArgumentsCount = queryArguments.size();

        Object[] args = new Object[queryArgumentsCount];
        for (int i = 0; i < queryArgumentsCount; i++) {
            QueryArgument argDescriptor =  queryArguments.get(i);

            if (argDescriptor.isRelayConnection()) {
                args[i] = connectionRequest;
            } else if (argDescriptor.isResolverSource() && !arguments.containsKey(sourceArgument.getName())) {
                args[i] = source;
            } else if (argDescriptor.isContext()) {
                args[i] = context;
            } else if (argDescriptor.isRelayId()) {
                String rawId = arguments.get(argDescriptor.getName()).toString();
                String id = rawId;
                try {
                    id = executionContext.relay.fromGlobalId(rawId).id;
                } catch (Exception e) {/*noop*/}
                args[i] = executionContext.idTypeMapper.deserialize(id, executable.getAnnotatedParameterTypes()[i].getType());
            } else {
                InputConverter argValueConverter = executionContext.converters.getInputConverter(argDescriptor.getJavaType());
                AnnotatedType argValueType = argValueConverter != null ? argValueConverter.getSubstituteType(argDescriptor.getJavaType()) : argDescriptor.getJavaType();
                Object argValue = executionContext.inputDeserializer.deserialize(arguments.get(argDescriptor.getName()), argValueType);
                args[i] = argValueConverter == null ? argValue : argValueConverter.convertInput(argValue);
            }
        }
        Object result = executable.execute(source, args);
//        if (result instanceof Collection) {
//            result = new ArrayList<>(((Collection<?>) result));
//        }
        OutputConverter resultConverter = executionContext.converters.getOutputConverter(this.getReturnType());
        return resultConverter == null ? result : resultConverter.convertOutput(result);
        //Wrap returned values for resolvers that don't directly return domain objects
//        if (isWrapped()) {
//            if (!Map.class.isAssignableFrom(result.getClass())) {
//                Map<String, Object> wrappedResult = new HashMap<>(1);
//                wrappedResult.put(wrappedAttribute, result);
//                return wrappedResult;
//            }
//            return result;
//        }
//        return result;
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
        return queryArguments.size() == 1 && queryArguments.get(0).isRelayId();
    }

    /**
     * Gets the generic Java type of the source object (object returned by the parent query),
     * if one is accepted by this resolver. Used to decide if this query can be nested inside another.
     *
     * @return The generic Java type of the source object, or null if this resolver does not accept one.
     */
    public Type getSourceType() {
        return sourceArgument == null ? null : sourceArgument.getJavaType().getType();
    }

    private boolean isWrapped() {
        return !(wrappedAttribute == null || wrappedAttribute.isEmpty());
    }

    public String getQueryName() {
        return queryName;
    }

    public String getQueryDescription() {
        return queryDescription;
    }

    public boolean isRelayId() {
        return relayId;
    }

    /**
     * Get the fingerprint of this resolver. Fingerprint uniquely identifies a resolver within a query.
     * It is based on the name of the query and all parameters this specific resolver accepts.
     * It is used to decide which resolver to invoke for the query, based on the provided arguments.
     *
     * @return The unique "fingerprint" string identifying this resolver
     */
    public Set<String> getFingerprints() {
        Set<String> fingerprints = new HashSet<>(sourceArgument == null ? 1 : 2);
        fingerprints.add(fingerprint(false));
        if (sourceArgument != null) {
            fingerprints.add(fingerprint(true));
        }
        return fingerprints;
    }

    public List<QueryArgument> getQueryArguments() {
        return queryArguments;
    }

    public AnnotatedType getReturnType() {
        return returnType;
    }

    public Executable getExecutable() {
        return executable;
    }

    private String fingerprint(boolean skipResolverSource) {
        StringBuilder fingerprint = new StringBuilder();
        Predicate<QueryArgument> sourceFilter = skipResolverSource ? arg -> !arg.isResolverSource() : arg -> true;
        queryArguments.stream()
                .filter(sourceFilter)
                .filter(arg -> !arg.isRelayConnection() && !arg.isContext())
                .map(QueryArgument::getName)
                .sorted()
                .forEach(fingerprint::append);
        return fingerprint.toString();
    }

    @Override
    public String toString() {
        return executable.toString();
    }
}
