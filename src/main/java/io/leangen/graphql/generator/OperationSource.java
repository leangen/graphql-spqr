package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Created by bojan.tomic on 7/10/16.
 */
public class OperationSource {

    private final Supplier<Object> serviceBeanSupplier;
    private final AnnotatedType javaType;
    private final Collection<ResolverBuilder> resolverBuilders;

    OperationSource(Supplier<Object> serviceBeanSupplier, AnnotatedType javaType, Collection<ResolverBuilder> resolverBuilders) {
        this.serviceBeanSupplier = serviceBeanSupplier;
        this.javaType = javaType;
        this.resolverBuilders = resolverBuilders;
    }

    OperationSource(AnnotatedType javaType, Collection<ResolverBuilder> resolverBuilders) {
        this(null, javaType, resolverBuilders);
    }

    Supplier<Object> getServiceBeanSupplier() {
        return serviceBeanSupplier;
    }

    AnnotatedType getJavaType() {
        return javaType;
    }

    Collection<ResolverBuilder> getResolverBuilders() {
        return resolverBuilders;
    }
}
