package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by bojan.tomic on 7/10/16.
 */
public class OperationSource {

    private final Supplier<Object> serviceBeanSupplier;
    private final AnnotatedType javaType;
    private final Class<?> exposedType;
    private final Collection<ResolverBuilder> resolverBuilders;

    OperationSource(Supplier<Object> serviceBeanSupplier, AnnotatedType javaType, Class<?> exposedType, Collection<ResolverBuilder> resolverBuilders) {
        this.serviceBeanSupplier = serviceBeanSupplier;
        this.javaType = javaType;
        this.exposedType = exposedType;
        this.resolverBuilders = resolverBuilders;
    }

    OperationSource(AnnotatedType javaType, Collection<ResolverBuilder> resolverBuilders) {
        this(null, javaType, null, resolverBuilders);
    }

    Supplier<Object> getServiceBeanSupplier() {
        return serviceBeanSupplier;
    }

    AnnotatedType getJavaType() {
        return javaType;
    }

    public Class<?> getExposedType() {
        return exposedType;
    }

    Collection<ResolverBuilder> getResolverBuilders() {
        return resolverBuilders.stream().filter(builder -> builder.supports(javaType)).collect(Collectors.toList());
    }
}
