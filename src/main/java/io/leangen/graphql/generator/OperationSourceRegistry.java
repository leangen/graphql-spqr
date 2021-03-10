package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

/**
 * Created by bojan.tomic on 7/12/16.
 */
public class OperationSourceRegistry {

    private final Collection<ResolverBuilder> topLevelResolverBuilders = new LinkedHashSet<>();
    private final Collection<ResolverBuilder> nestedResolverBuilders = new LinkedHashSet<>();
    private final Collection<OperationSource> operationSources = new HashSet<>();

    public void registerOperationSource(Supplier<Object> querySourceBeanSupplier, AnnotatedType beanType, Class<?> exposedType, Collection<ResolverBuilder> builders) {
        this.operationSources.add(new OperationSource(querySourceBeanSupplier, beanType, exposedType, Utils.defaultIfEmpty(builders, topLevelResolverBuilders)));
    }

    public void registerOperationSource(AnnotatedType serviceType, Collection<ResolverBuilder> builders) {
        this.operationSources.add(new OperationSource(serviceType, Utils.defaultIfEmpty(builders, topLevelResolverBuilders)));
    }

    public void registerGlobalResolverBuilders(Collection<ResolverBuilder> resolverDetectionStrategies) {
        this.topLevelResolverBuilders.addAll(resolverDetectionStrategies);
    }

    public void registerGlobalNestedResolverBuilders(Collection<ResolverBuilder> resolverDetectionStrategies) {
        this.nestedResolverBuilders.addAll(resolverDetectionStrategies);
    }

    public OperationSource nestedSourceForType(AnnotatedType domainType) {
        return new OperationSource(domainType, nestedResolverBuilders);
    }

    public Collection<OperationSource> getOperationSources() {
        return operationSources;
    }

    public boolean isEmpty() {
        return operationSources.isEmpty();
    }
}
