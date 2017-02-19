package io.leangen.graphql.query;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 1/29/17.
 */
public class QueryRunner {

    private Query query;
    private ValueMapper valueMapper;
    private GlobalContext globalContext;

    public QueryRunner(Query query, ValueMapper valueMapper, GlobalContext globalContext) {
        this.query = query;
        this.valueMapper = valueMapper;
        this.globalContext = globalContext;
    }

    public Object run(DataFetchingEnvironment env) {
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

        ResolutionContext resolutionContext = new ResolutionContext(
                env, new ConnectionRequest(connectionArguments), this.valueMapper, this.globalContext);

        QueryResolver resolver = this.query.getResolver(queryArguments.keySet());
        try {
            if (resolver == null) {
                if (queryArguments.size() == 0 && env.getSource() != null) {
                    return ClassUtils.getFieldValue(env.getSource(), query.getName());
                } else {
                    //TODO implement simple filtering here
                }
            } else {
                Object result = run(resolver, resolutionContext, queryArguments);
                return resolutionContext.convertOutput(result, resolver.getReturnType());
            }
            throw new GraphQLException("Resolver for query " + query.getName() + " accepting arguments: " 
                    + env.getArguments().keySet() + " not implemented");
        } catch (Exception e) {
            throw new GraphQLException("Query resolution exception", e);
        }
    }

    /**
     * Prepares input arguments by calling respective {@link io.leangen.graphql.generator.mapping.InputValueProvider}s
     * and invokes the underlying resolver method/field
     *
     * @param resolver The resolver to be invoked once the arguments are prepared
     * @param resolutionContext An object containing all contextual information needed during query resolution
     * @param rawArguments Raw input arguments provided by the client
     *
     * @return The result returned by the underlying method/field, potentially proxied and wrapped
     *
     * @throws InvocationTargetException If a reflective invocation of the underlying method/field fails
     * @throws IllegalAccessException If a reflective invocation of the underlying method/field is not allowed
     */
    private Object run(QueryResolver resolver, ResolutionContext resolutionContext, Map<String, Object> rawArguments) 
            throws InvocationTargetException, IllegalAccessException {

        int queryArgumentsCount = resolver.getQueryArguments().size();

        Object[] args = new Object[queryArgumentsCount];
        for (int i = 0; i < queryArgumentsCount; i++) {
            QueryArgument argDescriptor =  resolver.getQueryArguments().get(i);
            Object rawArgValue = rawArguments.get(argDescriptor.getName());

            args[i] = resolutionContext.getInputValue(rawArgValue, argDescriptor.getJavaType());
        }
        return resolver.resolve(resolutionContext.source, args);
    }
}
