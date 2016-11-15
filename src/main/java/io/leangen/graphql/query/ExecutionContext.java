package io.leangen.graphql.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import graphql.relay.Relay;
import graphql.schema.DataFetchingEnvironment;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLTypeHintProvider;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.proxy.ProxyFactory;
import io.leangen.graphql.generator.proxy.TypeHintProvider;
import io.leangen.graphql.metadata.DomainType;
import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 5/14/16.
 */
public class ExecutionContext {

    public final Relay relay;
    public final TypeRepository typeRepository;
    public final ProxyFactory proxyFactory;
    public final IdTypeMapper idTypeMapper;
    public final InputDeserializer inputDeserializer;
    public final ConverterRepository converters;

    public ExecutionContext(Relay relay, TypeRepository typeRepository, ProxyFactory proxyFactory, IdTypeMapper idTypeMapper,
                            InputDeserializer inputDeserializer, ConverterRepository converters) {
        this.relay = relay;
        this.typeRepository = typeRepository;
        this.proxyFactory = proxyFactory;
        this.idTypeMapper = idTypeMapper;
        this.inputDeserializer = inputDeserializer;
        this.converters = converters;
    }

    public Object proxyIfNeeded(DataFetchingEnvironment env, Object result, QueryResolver resolver) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        AnnotatedType type = resolver.getReturnType();
        List<TypeRepository.MappedType> mappedTypes = typeRepository.getOutputTypes(env.getFieldType().getName(), result.getClass());
        if (mappedTypes.isEmpty()) return result;

        if (mappedTypes.size() == 1) {
            String graphQLType = typeRepository.getOutputTypes(env.getFieldType().getName(), result.getClass()).get(0).graphQLType.getName();
            return proxyFactory.proxy(result, graphQLType);
        }
        if (type.isAnnotationPresent(GraphQLTypeHintProvider.class)) {
            TypeHintProvider hint = type.getAnnotation(GraphQLTypeHintProvider.class).value().newInstance();
            return proxyFactory.proxy(result, hint.getGraphQLTypeHint(result, env, relay, resolver));
        } else if (result.getClass().isAnnotationPresent(GraphQLTypeHintProvider.class)) {
            TypeHintProvider hint = result.getClass().getAnnotation(GraphQLTypeHintProvider.class).value().newInstance();
            return proxyFactory.proxy(result, hint.getGraphQLTypeHint(result, env, relay, resolver));
        } else if (ClassUtils.getRawType(type.getType()).isAnnotationPresent(GraphQLTypeHintProvider.class)) {
            TypeHintProvider hint = result.getClass().getAnnotation(GraphQLTypeHintProvider.class).value().newInstance();
            return proxyFactory.proxy(result, hint.getGraphQLTypeHint(result, env, relay, resolver));
        } else if (new DomainType(type).getName().equals(env.getFieldType().getName())) {
            try {
                AnnotatedType resolved = GenericTypeReflector.getExactSubType(type, result.getClass());
                if (resolved != null) {
                    return proxyFactory.proxy(result, new DomainType(resolved).getName());
                }
            } catch (Exception e) {/*no-op*/}
        }
        throw new IllegalStateException(String.format(
                "Exact GraphQL type for %s is unresolvable for object of type %s",
                env.getFieldType().getName(), result.getClass().getCanonicalName()));
    }
}
