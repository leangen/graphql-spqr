package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.strategy.query.ResolverExtractor;

import java.lang.reflect.AnnotatedType;
import java.util.*;

/**
 * Created by bojan.tomic on 7/12/16.
 */
public class QuerySourceRepository {

    public final Collection<ResolverExtractor> resolverExtractors = new HashSet<>();
    private final Collection<QuerySource> querySources = new HashSet<>();
    private final Map<AnnotatedType, QuerySource> domainQuerySources = new HashMap<>();

    public void registerSingletonQuerySource(Object querySourceBean, AnnotatedType beanType) {
        this.querySources.add(new QuerySource(querySourceBean, beanType, resolverExtractors));
    }

    public void registerSingletonQuerySource(Object querySourceBean, AnnotatedType beanType, Collection<ResolverExtractor> extractors) {
        this.querySources.add(new QuerySource(querySourceBean, beanType, extractors));
    }

    public void registerDomainQuerySource(AnnotatedType domainType) {
        this.domainQuerySources.put(domainType, new QuerySource(domainType, resolverExtractors));
    }

    public void registerDomainQuerySource(AnnotatedType domainType, Collection<ResolverExtractor> extractors) {
        this.domainQuerySources.put(domainType, new QuerySource(domainType, extractors));
    }

    public void registerGlobalQueryExtractors(ResolverExtractor... resolverExtractors) {
        Arrays.stream(resolverExtractors)
                .filter(resolverExtractor -> !this.resolverExtractors.contains(resolverExtractor)) //don't allow duplicates
                .forEach(this.resolverExtractors::add);
    }

    public QuerySource domainSourceForType(AnnotatedType domainType) {
        return domainQuerySources.computeIfAbsent(domainType, javaType -> new QuerySource(javaType, resolverExtractors));
    }

    public Collection<QuerySource> getQuerySources() {
        return querySources;
    }

    public Collection<QuerySource> getDomainQuerySources() {
        return domainQuerySources.values();
    }

    public boolean hasGlobalExtractors() {
        return !resolverExtractors.isEmpty();
    }

    public boolean isEmpty() {
        return querySources.isEmpty();
    }
}
