package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.query.ConnectionRequest;
import io.leangen.graphql.query.conversion.ConverterRepository;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Created by bojan.tomic on 7/17/16.
 */
public interface QueryResolverArgumentExtractor {

    List<QueryArgument> extractResolverArguments(List<String> parentPaths, Method resolverMethod, AnnotatedType enclosingType,
                                                 TypeMapperRepository typeMappers, ConverterRepository converters);

    default boolean isExplicitRelayConnectionArgument(Parameter parameter) {
        return ConnectionRequest.class.isAssignableFrom(parameter.getType());
    }
}
