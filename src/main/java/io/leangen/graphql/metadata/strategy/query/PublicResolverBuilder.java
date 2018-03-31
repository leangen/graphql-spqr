package io.leangen.graphql.metadata.strategy.query;

import graphql.execution.batched.Batched;
import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.execution.MethodInvoker;
import io.leangen.graphql.metadata.execution.SingletonMethodInvoker;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.reactivestreams.Publisher;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A resolver builder that exposes all public methods
 */
@SuppressWarnings("WeakerAccess")
public class PublicResolverBuilder extends FilteredResolverBuilder {

    private String[] basePackages;
    private boolean javaDeprecation;
    private Function<Method, String> descriptionMapper = method -> "";
    private Function<Method, String> deprecationReasonMapper = method -> javaDeprecation && method.isAnnotationPresent(Deprecated.class) ? "" : null;

    public PublicResolverBuilder(String... basePackages) {
        this(new DefaultTypeTransformer(false, false), basePackages);
    }

    public PublicResolverBuilder(TypeTransformer transformer, String... basePackages) {
        this.transformer = Objects.requireNonNull(transformer);
        this.operationNameGenerator = new MethodOperationNameGenerator();
        this.argumentBuilder = new AnnotatedArgumentBuilder(transformer);
        withBasePackages(basePackages);
        withJavaDeprecationRespected(true);
        withDefaultFilters();
    }

    public PublicResolverBuilder withBasePackages(String... basePackages) {
        this.basePackages = basePackages;
        return this;
    }

    public PublicResolverBuilder withJavaDeprecationRespected(boolean javaDeprecation) {
        this.javaDeprecation = javaDeprecation;
        return this;
    }

    public PublicResolverBuilder withDescriptionMapper(Function<Method, String> descriptionMapper) {
        this.descriptionMapper = descriptionMapper;
        return this;
    }

    public PublicResolverBuilder withDeprecationReasonMapper(Function<Method, String> deprecationReasonMapper) {
        this.deprecationReasonMapper = deprecationReasonMapper;
        return this;
    }

    @Override
    public Collection<Resolver> buildQueryResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy) {
        return buildQueryResolvers(querySourceBean, beanType, inclusionStrategy, getFilters());
    }

    @Override
    public Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy) {
        return buildMutationResolvers(querySourceBean, beanType, inclusionStrategy, getFilters());
    }

    @Override
    public Collection<Resolver> buildSubscriptionResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy) {
        return buildSubscriptionResolvers(querySourceBean, beanType, inclusionStrategy, getFilters());
    }

    private Collection<Resolver> buildQueryResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy, List<Predicate<Member>> filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        if (rawType.isArray() || rawType.isPrimitive()) return Collections.emptyList();
        return Arrays.stream(rawType.getMethods())
                .filter(method -> isPackageAcceptable(method, rawType))
                .filter(this::isQuery)
                .filter(method -> inclusionStrategy.includeOperation(method, getReturnType(method, beanType)))
                .filter(filters.stream().reduce(Predicate::and).orElse(ACCEPT_ALL))
                .map(method -> new Resolver(
                        operationNameGenerator.generateQueryName(method, beanType, querySourceBean),
                        descriptionMapper.apply(method),
                        deprecationReasonMapper.apply(method),
                        method.isAnnotationPresent(Batched.class),
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        getReturnType(method, beanType),
                        argumentBuilder.buildResolverArguments(method, beanType, inclusionStrategy),
                        method.isAnnotationPresent(GraphQLComplexity.class) ? method.getAnnotation(GraphQLComplexity.class).value() : null
                ))
                .collect(Collectors.toList());
    }

    private Collection<Resolver> buildMutationResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy, List<Predicate<Member>> filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        if (rawType.isArray()|| rawType.isPrimitive()) return Collections.emptyList();
        return Arrays.stream(rawType.getMethods())
                .filter(method -> isPackageAcceptable(method, rawType))
                .filter(this::isMutation)
                .filter(method -> inclusionStrategy.includeOperation(method, getReturnType(method, beanType)))
                .filter(filters.stream().reduce(Predicate::and).orElse(ACCEPT_ALL))
                .map(method -> new Resolver(
                        operationNameGenerator.generateMutationName(method, beanType, querySourceBean),
                        descriptionMapper.apply(method),
                        deprecationReasonMapper.apply(method),
                        false,
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        getReturnType(method, beanType),
                        argumentBuilder.buildResolverArguments(method, beanType, inclusionStrategy),
                        method.isAnnotationPresent(GraphQLComplexity.class) ? method.getAnnotation(GraphQLComplexity.class).value() : null
                ))
                .collect(Collectors.toList());
    }

    private Collection<Resolver> buildSubscriptionResolvers(Object querySourceBean, AnnotatedType beanType, InclusionStrategy inclusionStrategy, List<Predicate<Member>> filters) {
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        if (rawType.isArray()|| rawType.isPrimitive()) return Collections.emptyList();
        return Arrays.stream(rawType.getMethods())
                .filter(method -> isPackageAcceptable(method, rawType))
                .filter(this::isSubscription)
                .filter(method -> inclusionStrategy.includeOperation(method, getReturnType(method, beanType)))
                .filter(filters.stream().reduce(Predicate::and).orElse(ACCEPT_ALL))
                .map(method -> new Resolver(
                        operationNameGenerator.generateSubscriptionName(method, beanType, querySourceBean),
                        descriptionMapper.apply(method),
                        deprecationReasonMapper.apply(method),
                        false,
                        querySourceBean == null ? new MethodInvoker(method, beanType) : new SingletonMethodInvoker(querySourceBean, method, beanType),
                        getReturnType(method, beanType),
                        argumentBuilder.buildResolverArguments(method, beanType, inclusionStrategy),
                        method.isAnnotationPresent(GraphQLComplexity.class) ? method.getAnnotation(GraphQLComplexity.class).value() : null
                ))
                .collect(Collectors.toList());
    }

    protected boolean isQuery(Method method) {
        return !isMutation(method) && !isSubscription(method);
    }

    protected boolean isMutation(Method method) {
        return method.getReturnType() == void.class;
    }

    protected boolean isSubscription(Method method) {
        return method.getReturnType() == Publisher.class;
    }

    protected boolean isPackageAcceptable(Method method, Class<?> beanType) {
        String[] basePackages = new String[0];
        if (Utils.isArrayNotEmpty(this.basePackages)) {
            basePackages = Arrays.stream(this.basePackages).filter(Utils::isNotEmpty).toArray(String[]::new);
        } else if (beanType.getPackage() != null) {
            basePackages = new String[] {beanType.getPackage().getName()};
        }
        return method.getDeclaringClass().equals(beanType)
                || Arrays.stream(basePackages).anyMatch(basePackage -> ClassUtils.isSubPackage(method.getDeclaringClass().getPackage(), basePackage));
    }
}
