package io.leangen.graphql.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.GraphQLRuntime;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

/**
 * Created by bojan.tomic on 1/29/17.
 */
public class OperationExecutor {

    private final Operation operation;
    private final ValueMapper valueMapper;
    private final GlobalEnvironment globalEnvironment;

    private static final Logger log = LoggerFactory.getLogger(OperationExecutor.class); 
    
    public OperationExecutor(Operation operation, ValueMapper valueMapper, GlobalEnvironment globalEnvironment) {
        this.operation = operation;
        this.valueMapper = valueMapper;
        this.globalEnvironment = globalEnvironment;
    }

    public Object execute(DataFetchingEnvironment env) {
        Resolver resolver;
        if (env.getContext() instanceof GraphQLRuntime.ContextWrapper) {
            GraphQLRuntime.ContextWrapper context = env.getContext();
            if (env.getArguments().get("clientMutationId") != null) {
                context.putExtension("clientMutationId", env.getArguments().get("clientMutationId"));
            }
        }
        
        if (this.operation.getResolvers().size() == 1) {
            resolver = this.operation.getResolvers().iterator().next();
        } else {
            String[] nonNullArgumentNames = env.getArguments().entrySet().stream()
                    .filter(arg -> arg.getValue() != null)
                    .map(Map.Entry::getKey)
                    .toArray(String[]::new);

            resolver = this.operation.getResolver(nonNullArgumentNames);
        }
        try {
            if (resolver == null) {
                throw new GraphQLException("Resolver for operation " + operation.getName() + " accepting arguments: "
                        + env.getArguments().keySet() + " not implemented");
            } else {
                ResolutionEnvironment resolutionEnvironment = new ResolutionEnvironment(env, this.valueMapper, this.globalEnvironment);
                Object result = execute(resolver, resolutionEnvironment, env.getArguments());
                return resolutionEnvironment.convertOutput(result, resolver.getReturnType());
            }
        } catch (Exception e) {
            log.error("Operation resolution exception", e);
            throw new GraphQLException("Operation resolution exception", e);
        }
    }

    /**
     * Prepares input arguments by calling respective {@link ArgumentInjector}s
     * and invokes the underlying resolver method/field
     *
     * @param resolver The resolver to be invoked once the arguments are prepared
     * @param resolutionEnvironment An object containing all contextual information needed during operation resolution
     * @param rawArguments Raw input arguments provided by the client
     *
     * @return The result returned by the underlying method/field, potentially proxied and wrapped
     *
     * @throws InvocationTargetException If a reflective invocation of the underlying method/field fails
     * @throws IllegalAccessException If a reflective invocation of the underlying method/field is not allowed
     */
    private Object execute(Resolver resolver, ResolutionEnvironment resolutionEnvironment, Map<String, Object> rawArguments)
            throws InvocationTargetException, IllegalAccessException {

        int queryArgumentsCount = resolver.getArguments().size();

        Object[] args = new Object[queryArgumentsCount];
        for (int i = 0; i < queryArgumentsCount; i++) {
            OperationArgument argDescriptor =  resolver.getArguments().get(i);
            Object rawArgValue = rawArguments.get(argDescriptor.getName());

            args[i] = resolutionEnvironment.getInputValue(rawArgValue, argDescriptor.getJavaType());
        }
        return resolver.resolve(resolutionEnvironment.context, args);
    }
}
