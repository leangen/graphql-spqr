package io.leangen.graphql.execution;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

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
        Resolver resolver;
        if (this.operation.getResolvers().size() == 1) {
            resolver = this.operation.getResolvers().iterator().next();
        } else {
            Set<String> nonNullArgumentNames = env.getArguments().entrySet().stream()
                    .filter(arg -> arg.getValue() != null)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            resolver = this.operation.getResolver(nonNullArgumentNames);
        }
        try {
            if (resolver == null) {
                throw new GraphQLException("Resolver for operation " + operation.getName() + " accepting arguments: "
                        + env.getArguments().keySet() + " not implemented");
            } else {
                ResolutionContext resolutionContext = new ResolutionContext(env, this.valueMapper, this.globalContext);
                Object result = execute(resolver, resolutionContext, env.getArguments());
                return resolutionContext.convertOutput(result, resolver.getReturnType());
            }
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
