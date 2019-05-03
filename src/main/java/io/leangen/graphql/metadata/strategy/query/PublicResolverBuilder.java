package io.leangen.graphql.metadata.strategy.query;

import graphql.execution.batched.Batched;
import graphql.language.OperationDefinition;
import io.leangen.graphql.annotations.GraphQLComplexity;
import io.leangen.graphql.generator.JavaDeprecationMappingConfig;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.execution.FieldAccessor;
import io.leangen.graphql.metadata.execution.FixedMethodInvoker;
import io.leangen.graphql.metadata.execution.MethodInvoker;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.value.Property;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.ReservedStrings;
import io.leangen.graphql.util.Utils;
import org.reactivestreams.Publisher;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A resolver builder that exposes all public methods
 */
@SuppressWarnings("WeakerAccess")
public class PublicResolverBuilder extends AbstractResolverBuilder {

    private String[] basePackages;
    private JavaDeprecationMappingConfig javaDeprecationConfig;

    public PublicResolverBuilder() {
        this(new String[0]);
    }

    public PublicResolverBuilder(String... basePackages) {
        this.operationInfoGenerator = new DeprecationAwareOperationInfoGenerator();
        this.argumentBuilder = new AnnotatedArgumentBuilder();
        this.propertyElementReducer = AbstractResolverBuilder::mergePropertyElements;
        withBasePackages(basePackages);
        withJavaDeprecation(new JavaDeprecationMappingConfig(true, "Deprecated"));
        withDefaultFilters();
    }

    public PublicResolverBuilder withBasePackages(String... basePackages) {
        this.basePackages = basePackages;
        return this;
    }

    /**
     * Sets whether the {@code Deprecated} annotation should map to GraphQL deprecation
     *
     * @param javaDeprecation Whether the {@code Deprecated} maps to GraphQL deprecation
     * @return This builder instance to allow chained calls
     */
    public PublicResolverBuilder withJavaDeprecationRespected(boolean javaDeprecation) {
        this.javaDeprecationConfig = new JavaDeprecationMappingConfig(javaDeprecation, "Deprecated");
        return this;
    }

    /**
     * Sets whether and how the {@code Deprecated} annotation should map to GraphQL deprecation
     *
     * @param javaDeprecationConfig Configures if and how {@code Deprecated} maps to GraphQL deprecation
     * @return This builder instance to allow chained calls
     */
    public PublicResolverBuilder withJavaDeprecation(JavaDeprecationMappingConfig javaDeprecationConfig) {
        this.javaDeprecationConfig = javaDeprecationConfig;
        return this;
    }

    @Override
    public Collection<Resolver> buildQueryResolvers(ResolverBuilderParams params) {
        Set<Property> properties = ClassUtils.getProperties(ClassUtils.getRawType(params.getBeanType().getType()));
        Collection<Resolver> propertyAccessors = buildPropertyAccessors(properties.stream(), params);
        Collection<Resolver> methodInvokers = buildMethodInvokers(params, (method, par) -> isQuery(method, par) && properties.stream().noneMatch(prop -> prop.getGetter().equals(method)), OperationDefinition.Operation.QUERY, true);
        Collection<Resolver> fieldAccessors = buildFieldAccessors(params);
        return Utils.concat(methodInvokers.stream(), propertyAccessors.stream(), fieldAccessors.stream()).collect(Collectors.toSet());
    }

    @Override
    public Collection<Resolver> buildMutationResolvers(ResolverBuilderParams params) {
        return buildMethodInvokers(params, this::isMutation, OperationDefinition.Operation.MUTATION, false);
    }

    @Override
    public Collection<Resolver> buildSubscriptionResolvers(ResolverBuilderParams params) {
        return buildMethodInvokers(params, this::isSubscription, OperationDefinition.Operation.SUBSCRIPTION, false);
    }

    private Collection<Resolver> buildMethodInvokers(ResolverBuilderParams params, BiPredicate<Method, ResolverBuilderParams> filter, OperationDefinition.Operation operation, boolean batchable) {
        MessageBundle messageBundle = params.getEnvironment().messageBundle;
        AnnotatedType beanType = params.getBeanType();
        Supplier<Object> querySourceBean = params.getQuerySourceBeanSupplier();
        Class<?> rawType = ClassUtils.getRawType(beanType.getType());
        if (rawType.isArray() || rawType.isPrimitive()) return Collections.emptyList();

        return Arrays.stream(rawType.getMethods())
                .filter(method -> filter.test(method, params))
                .filter(method -> params.getInclusionStrategy().includeOperation(method, getReturnType(method, params)))
                .filter(getFilters().stream().reduce(Predicate::and).orElse(ACCEPT_ALL))
                .map(method -> {
                    TypedElement element = new TypedElement(getReturnType(method, params), method);
                    OperationInfoGeneratorParams infoParams = new OperationInfoGeneratorParams(element, beanType, querySourceBean, messageBundle, operation);
                    return new Resolver(
                            messageBundle.interpolate(operationInfoGenerator.name(infoParams)),
                            messageBundle.interpolate(operationInfoGenerator.description(infoParams)),
                            messageBundle.interpolate(ReservedStrings.decode(operationInfoGenerator.deprecationReason(infoParams))),
                            batchable && method.isAnnotationPresent(Batched.class),
                            querySourceBean == null ? new MethodInvoker(method, beanType) : new FixedMethodInvoker(querySourceBean, method, beanType),
                            element,
                            argumentBuilder.buildResolverArguments(
                                    new ArgumentBuilderParams(method, beanType, params.getInclusionStrategy(), params.getTypeTransformer(), params.getEnvironment())),
                            method.isAnnotationPresent(GraphQLComplexity.class) ? method.getAnnotation(GraphQLComplexity.class).value() : null
                    );
                })
                .collect(Collectors.toList());
    }

    private Collection<Resolver> buildPropertyAccessors(Stream<Property> properties, ResolverBuilderParams params) {
        MessageBundle messageBundle = params.getEnvironment().messageBundle;
        AnnotatedType beanType = params.getBeanType();
        Predicate<Member> mergedFilters = getFilters().stream().reduce(Predicate::and).orElse(ACCEPT_ALL);

        return properties
                .filter(prop -> isQuery(prop, params))
                .filter(prop -> mergedFilters.test(prop.getField()) && mergedFilters.test(prop.getGetter()))
                .map(prop -> new AbstractMap.SimpleEntry<>(prop.getGetter(), propertyElementReducer.apply(new TypedElement(getFieldType(prop.getField(), params), prop.getField()), new TypedElement(getReturnType(prop.getGetter(), params), prop.getGetter()))))
                .filter(pair -> params.getInclusionStrategy().includeOperation(pair.getValue(), pair.getValue().getJavaType()))
                .map(pair -> {
                    Method getter = pair.getKey();
                    TypedElement element = pair.getValue();
                    OperationInfoGeneratorParams infoParams = new OperationInfoGeneratorParams(element, beanType, params.getQuerySourceBeanSupplier(), messageBundle, OperationDefinition.Operation.QUERY);
                    return new Resolver(
                            messageBundle.interpolate(operationInfoGenerator.name(infoParams)),
                            messageBundle.interpolate(operationInfoGenerator.description(infoParams)),
                            messageBundle.interpolate(ReservedStrings.decode(operationInfoGenerator.deprecationReason(infoParams))),
                            element.isAnnotationPresent(Batched.class),
                            params.getQuerySourceBeanSupplier() == null ? new MethodInvoker(getter, beanType) : new FixedMethodInvoker(params.getQuerySourceBeanSupplier(), getter, beanType),
                            element,
                            argumentBuilder.buildResolverArguments(new ArgumentBuilderParams(getter, beanType, params.getInclusionStrategy(), params.getTypeTransformer(), params.getEnvironment())),
                            element.isAnnotationPresent(GraphQLComplexity.class) ? element.getAnnotation(GraphQLComplexity.class).value() : null
                    );
                })
                .collect(Collectors.toSet());
    }

    private Collection<Resolver> buildFieldAccessors(ResolverBuilderParams params) {
        MessageBundle messageBundle = params.getEnvironment().messageBundle;
        AnnotatedType beanType = params.getBeanType();

        return Arrays.stream(ClassUtils.getRawType(beanType.getType()).getFields())
                .filter(field -> isQuery(field, params))
                .filter(getFilters().stream().reduce(Predicate::and).orElse(ACCEPT_ALL))
                .filter(field -> params.getInclusionStrategy().includeOperation(field, getFieldType(field, params)))
                .map(field -> {
                    TypedElement element = new TypedElement(getFieldType(field, params), field);
                    OperationInfoGeneratorParams infoParams = new OperationInfoGeneratorParams(element, beanType, params.getQuerySourceBeanSupplier(), messageBundle, OperationDefinition.Operation.QUERY);
                    return new Resolver(
                            messageBundle.interpolate(operationInfoGenerator.name(infoParams)),
                            messageBundle.interpolate(operationInfoGenerator.description(infoParams)),
                            messageBundle.interpolate(ReservedStrings.decode(operationInfoGenerator.deprecationReason(infoParams))),
                            false,
                            new FieldAccessor(field, beanType),
                            element,
                            Collections.emptyList(),
                            field.isAnnotationPresent(GraphQLComplexity.class) ? field.getAnnotation(GraphQLComplexity.class).value() : null
                    );
                })
                .collect(Collectors.toSet());
    }

    protected boolean isQuery(Method method, ResolverBuilderParams params) {
        return isPackageAcceptable(method, params) && !isMutation(method, params) && !isSubscription(method, params);
    }

    protected boolean isQuery(Field field, ResolverBuilderParams params) {
        return isPackageAcceptable(field, params);
    }

    protected boolean isQuery(Property property, ResolverBuilderParams params) {
        return isQuery(property.getGetter(), params);
    }

    protected boolean isMutation(Method method, ResolverBuilderParams params) {
        return isPackageAcceptable(method, params) && method.getReturnType() == void.class;
    }

    protected boolean isSubscription(Method method, ResolverBuilderParams params) {
        return isPackageAcceptable(method, params) && Publisher.class.isAssignableFrom(method.getReturnType());
    }

    protected boolean isPackageAcceptable(Member method, ResolverBuilderParams params) {
        Class<?> beanType = ClassUtils.getRawType(params.getBeanType().getType());
        String[] defaultPackages = params.getBasePackages();
        String[] basePackages = new String[0];
        if (Utils.isArrayNotEmpty(this.basePackages)) {
            basePackages = this.basePackages;
        } else if (Utils.isArrayNotEmpty(defaultPackages)) {
            basePackages = defaultPackages;
        } else if (beanType.getPackage() != null) {
            basePackages = new String[] {beanType.getPackage().getName()};
        }
        basePackages = Arrays.stream(basePackages).filter(Utils::isNotEmpty).toArray(String[]::new); //remove the default package
        return method.getDeclaringClass().equals(beanType)
                || Arrays.stream(basePackages).anyMatch(basePackage -> ClassUtils.isSubPackage(method.getDeclaringClass().getPackage(), basePackage));
    }

    private class DeprecationAwareOperationInfoGenerator extends DefaultOperationInfoGenerator {
        @Override
        public String deprecationReason(OperationInfoGeneratorParams params) {
            String explicit = ReservedStrings.decode(super.deprecationReason(params));
            if (explicit == null && javaDeprecationConfig.enabled && params.getElement().isAnnotationPresent(Deprecated.class)) {
                return javaDeprecationConfig.deprecationReason;
            }
            return explicit;
        }
    }
}
