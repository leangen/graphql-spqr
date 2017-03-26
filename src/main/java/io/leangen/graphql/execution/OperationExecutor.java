package io.leangen.graphql.execution;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 1/29/17.
 */
public class OperationExecutor {

    private Operation operation;
    private ValueMapper valueMapper;
    private GlobalContext globalContext;

    public OperationExecutor(Operation operation, ValueMapper valueMapper, GlobalContext globalContext) {
        this.operation = operation;
        this.valueMapper = valueMapper;
        this.globalContext = globalContext;
    }

    public Object execute(DataFetchingEnvironment env) {
        Map<String, Object> queryArguments = new HashMap<>();
        Map<String, Object> connectionArguments = new HashMap<>();

        env.getArguments().forEach((key, value) -> {
            if (value != null) {
                if (ConnectionRequest.isConnectionArgumentName(key)) {
                    connectionArguments.put(key, value);
                } else {
                    queryArguments.put(key, value);
                }
            }
        });

        ResolutionContext resolutionContext = new ResolutionContext(
                env, new ConnectionRequest(connectionArguments), this.valueMapper, this.globalContext);

        Resolver resolver = this.operation.getResolver(queryArguments.keySet());
        try {
            if (resolver == null) {
                if (queryArguments.size() == 0 && env.getSource() != null) {
                    return ClassUtils.getFieldValue(env.getSource(), operation.getName());
                } else {
                    //TODO implement simple filtering here
                }
            } else {
                Object result = execute(resolver, resolutionContext, queryArguments);
                return resolutionContext.convertOutput(result, resolver.getReturnType());
            }
            throw new GraphQLException("Resolver for operation " + operation.getName() + " accepting arguments: " 
                    + env.getArguments().keySet() + " not implemented");
        } catch (Exception e) {
            throw new GraphQLException("Operation resolution exception", e);
        }
    }

    /**
     * Prepares input arguments by calling respective {@link ArgumentInjector}s
     * and invokes the underlying resolver method/field
     *
     * @param resolver The resolver to be invoked once the arguments are prepared
     * @param resolutionContext An object containing all contextual information needed during operation resolution
     * @param rawArguments Raw input arguments provided by the client
     *
     * @return The result returned by the underlying method/field, potentially proxied and wrapped
     *
     * @throws InvocationTargetException If a reflective invocation of the underlying method/field fails
     * @throws IllegalAccessException If a reflective invocation of the underlying method/field is not allowed
     */
    private Object execute(Resolver resolver, ResolutionContext resolutionContext, Map<String, Object> rawArguments) 
            throws InvocationTargetException, IllegalAccessException {

        int queryArgumentsCount = resolver.getArguments().size();

        Object[] args = new Object[queryArgumentsCount];
        for (int i = 0; i < queryArgumentsCount; i++) {
            OperationArgument argDescriptor =  resolver.getArguments().get(i);
            Object rawArgValue = rawArguments.get(argDescriptor.getName());

            args[i] = resolutionContext.getInputValue(rawArgValue, argDescriptor.getJavaType());
        }
        return resolver.resolve(resolutionContext.source, args);
    }
}
