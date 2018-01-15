package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import io.leangen.geantyref.AnnotatedTypeMap;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;

import static java.util.Collections.addAll;

/**
 * Created by bojan.tomic on 7/12/16.
 */
public class OperationSourceRepository {

    private final Collection<ResolverBuilder> topLevelResolverBuilders = new LinkedHashSet<>();
    private final Collection<ResolverBuilder> nestedResolverBuilders = new LinkedHashSet<>();
    private final Collection<OperationSource> operationSources = new HashSet<>();
    private final Map<AnnotatedType, OperationSource> nestedOperationSources = new AnnotatedTypeMap<>();

    public void registerOperationSource(Object querySourceBean, AnnotatedType beanType) {
        this.operationSources.add(new OperationSource(querySourceBean, beanType, topLevelResolverBuilders));
    }

    public void registerOperationSource(Object querySourceBean, AnnotatedType beanType, Collection<ResolverBuilder> extractors) {
        this.operationSources.add(new OperationSource(querySourceBean, beanType, extractors));
    }

    public void registerOperationSource(AnnotatedType serviceType) {
        this.operationSources.add(new OperationSource(serviceType, topLevelResolverBuilders));
    }
    
    public void registerOperationSource(AnnotatedType serviceType, Collection<ResolverBuilder> strategies) {
        this.operationSources.add(new OperationSource(serviceType, strategies));
    }
    
    public void registerNestedOperationSource(AnnotatedType domainType) {
        this.nestedOperationSources.put(domainType, new OperationSource(domainType, nestedResolverBuilders));
    }

    public void registerNestedOperationSource(AnnotatedType domainType, Collection<ResolverBuilder> extractors) {
        this.nestedOperationSources.put(domainType, new OperationSource(domainType, extractors));
    }

    public void registerGlobalResolverBuilders(ResolverBuilder... resolverDetectionStrategies) {
        addAll(this.topLevelResolverBuilders, resolverDetectionStrategies);
    }
    
    public void registerGlobalNestedResolverBuilders(ResolverBuilder... resolverDetectionStrategies) {
        addAll(this.nestedResolverBuilders, resolverDetectionStrategies);
    }

    public OperationSource nestedSourceForType(AnnotatedType domainType) {
        return nestedOperationSources.computeIfAbsent(domainType, javaType -> new OperationSource(javaType, nestedResolverBuilders));
    }

    public Collection<OperationSource> getOperationSources() {
        return operationSources;
    }

    public Collection<OperationSource> getNestedOperationSources() {
        return nestedOperationSources.values();
    }

    public boolean hasGlobalResolverBuilders() {
        return !topLevelResolverBuilders.isEmpty();
    }
    
    public boolean hasGlobalNestedResolverBuilders() {
        return !nestedResolverBuilders.isEmpty();
    }

    public boolean isEmpty() {
        return operationSources.isEmpty();
    }
}
