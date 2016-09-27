package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.metadata.QueryResolver;
import io.leangen.graphql.query.conversion.ConverterRepository;
import io.leangen.graphql.query.execution.Executable;
import io.leangen.graphql.query.execution.FieldAccessor;
import io.leangen.graphql.query.execution.MethodInvoker;
import io.leangen.graphql.query.execution.SingletonMethodInvoker;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by bojan.tomic on 6/7/16.
 */
public class AnnotatedResolverExtractor implements ResolverExtractor {

    private final QueryNameGenerator queryNameGenerator;
    private final QueryResolverArgumentExtractor argumentExtractor;

    public AnnotatedResolverExtractor() {
        this(new DelegatingQueryNameGenerator(new AnnotatedQueryNameGenerator(), new MethodQueryNameGenerator()), new AnnotatedArgumentExtractor());
    }

    public AnnotatedResolverExtractor(QueryNameGenerator queryNameGenerator, QueryResolverArgumentExtractor argumentExtractor) {
        this.queryNameGenerator = queryNameGenerator;
        this.argumentExtractor = argumentExtractor;
    }

    @Override
    public Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType, TypeMapperRepository typeMappers, ConverterRepository converters) {
        return extractQueryResolvers(querySourceBean, beanType, typeMappers, converters, acceptAll);
    }

    @Override
    public Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType, TypeMapperRepository typeMappers, ConverterRepository converters) {
        return extractMutationResolvers(querySourceBean, beanType, typeMappers, converters, acceptAll);
    }

    @Override
    public Collection<QueryResolver> extractQueryResolvers(Object querySourceBean, AnnotatedType beanType, TypeMapperRepository typeMappers, ConverterRepository converters, Predicate<Member>... filters) {
        Stream<QueryResolver> methodInvokers = ClassUtils.getAnnotatedMethods(ClassUtils.getRawType(beanType.getType()), GraphQLQuery.class).stream()
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(method -> toQueryResolver(method, querySourceBean, beanType, typeMappers, converters));
        Stream<QueryResolver> fieldAccessors = ClassUtils.getAnnotatedFields(ClassUtils.getRawType(beanType.getType()), GraphQLQuery.class).stream()
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(field -> toQueryResolver(field, beanType, typeMappers, converters));
        return Stream.concat(methodInvokers, fieldAccessors).collect(Collectors.toSet());

    }

    @Override
    public Collection<QueryResolver> extractMutationResolvers(Object querySourceBean, AnnotatedType beanType, TypeMapperRepository typeMappers, ConverterRepository converters, Predicate<Member>... filters) {
        return ClassUtils.getAnnotatedMethods(ClassUtils.getRawType(beanType.getType()), GraphQLMutation.class).stream()
                .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(acceptAll))
                .map(method -> toMutationResolver(method, querySourceBean, beanType, typeMappers, converters)).collect(Collectors.toSet());
    }

    private QueryResolver toQueryResolver(Method method, Object querySourceBean, AnnotatedType beanType,
                                          TypeMapperRepository typeMappers, ConverterRepository converters) {

        String queryName = queryNameGenerator.generateQueryName(method, beanType);
        Executable executable = querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType);
        registerMapperAndConverter(executable, queryName, typeMappers, converters);
        return new QueryResolver(
                queryName,
                method.getAnnotation(GraphQLQuery.class).description(),
                method.getAnnotatedReturnType().isAnnotationPresent(RelayId.class),
                executable,
                argumentExtractor.extractResolverArguments(allPaths(executable.getParentTrails(), queryName), method, beanType, typeMappers, converters));
    }

    private QueryResolver toQueryResolver(Field field, AnnotatedType beanType, TypeMapperRepository typeMappers, ConverterRepository converters) {
        String queryName = queryNameGenerator.generateQueryName(field, beanType);
        Executable executable = new FieldAccessor(field, beanType);
        registerMapperAndConverter(executable, queryName, typeMappers, converters);
        return new QueryResolver(
                queryName,
                field.getAnnotation(GraphQLQuery.class).description(),
                field.getAnnotatedType().isAnnotationPresent(RelayId.class),
                executable,
                Collections.emptyList());
    }

    private QueryResolver toMutationResolver(Method method, Object querySourceBean, AnnotatedType beanType,
                                             TypeMapperRepository typeMappers, ConverterRepository converters) {

        String mutationName = queryNameGenerator.generateMutationName(method, beanType);
        Executable executable = querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType);
        registerMapperAndConverter(executable, mutationName, typeMappers, converters);
        return new QueryResolver(
                mutationName,
                method.getAnnotation(GraphQLMutation.class).description(),
                method.getAnnotatedReturnType().isAnnotationPresent(RelayId.class),
                executable,
                argumentExtractor.extractResolverArguments(allPaths(executable.getParentTrails(), mutationName), method, beanType, typeMappers, converters));
    }

    private List<String> allPaths(Set<List<String>> parentTrails, String queryName) {
        return parentTrails.stream()
                .map(trail -> String.join(".", trail) + "." + queryName)
                .collect(Collectors.toList());
    }

    private void registerMapperAndConverter(Executable executable, String queryName, TypeMapperRepository typeMappers, ConverterRepository converters) {
        allPaths(executable.getParentTrails(), queryName).forEach(path -> {
            typeMappers.registerTypeMapperForPath(path, executable.getReturnType());
            converters.registerOutputConverterForPath(path, executable.getReturnType());
        });
    }
}
