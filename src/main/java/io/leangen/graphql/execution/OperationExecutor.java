package io.leangen.graphql.execution;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static io.leangen.graphql.util.GraphQLUtils.CLIENT_MUTATION_ID;

/**
 * Created by bojan.tomic on 1/29/17.
 */
public class OperationExecutor {

    private final Operation operation;
    private final ValueMapper valueMapper;
    private final GlobalEnvironment globalEnvironment;

    public OperationExecutor(Operation operation, ValueMapper valueMapper, GlobalEnvironment globalEnvironment) {
        this.operation = operation;
        this.valueMapper = valueMapper;
        this.globalEnvironment = globalEnvironment;
    }

    public Object execute(DataFetchingEnvironment env) {
        Resolver resolver;
        if (env.getContext() instanceof ContextWrapper) {
            ContextWrapper context = env.getContext();
            if (env.getArguments().get(CLIENT_MUTATION_ID) != null) {
                context.setClientMutationId((String) env.getArguments().get(CLIENT_MUTATION_ID));
            }
        }

        resolver = this.operation.getApplicableResolver(env.getArguments().keySet());
        if (resolver == null) {
            throw new GraphQLException("Resolver for operation " + operation.getName() + " accepting arguments: "
                    + env.getArguments().keySet() + " not implemented");
        }
        ResolutionEnvironment resolutionEnvironment = new ResolutionEnvironment(env, this.valueMapper, this.globalEnvironment);
        try {
            Object result = execute(resolver, resolutionEnvironment, env.getArguments());
            return resolutionEnvironment.convertOutput(result, resolver.getReturnType());
        } catch (Throwable throwable) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else {
                throw new RuntimeException(throwable);
            }
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
            throws Throwable {

        int queryArgumentsCount = resolver.getArguments().size();

        Object[] args = new Object[queryArgumentsCount];
        for (int i = 0; i < queryArgumentsCount; i++) {
            OperationArgument argDescriptor =  resolver.getArguments().get(i);
            Object rawArgValue = rawArguments.get(argDescriptor.getName());

            args[i] = resolutionEnvironment.getInputValue(rawArgValue, argDescriptor.getJavaType(), argDescriptor.getParameter());
        }
        return resolver.resolve(resolutionEnvironment.context, args);
    }
}
