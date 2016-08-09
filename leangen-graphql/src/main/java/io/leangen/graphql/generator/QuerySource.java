package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;

import io.leangen.graphql.metadata.strategy.query.ResolverExtractor;

/**
 * Created by bojan.tomic on 7/10/16.
 */
public class QuerySource {

	private final Object querySourceBean;
	private final AnnotatedType javaType;
	private final Collection<ResolverExtractor> extractors;

	public QuerySource(Object querySourceBean, AnnotatedType javaType, Collection<ResolverExtractor> extractors) {
		this.querySourceBean = querySourceBean;
		this.javaType = javaType;
		this.extractors = extractors;
	}

	public QuerySource(AnnotatedType javaType, Collection<ResolverExtractor> extractors) {
		this(null, javaType, extractors);
	}

	public Object getQuerySourceBean() {
		return querySourceBean;
	}

	public AnnotatedType getJavaType() {
		return javaType;
	}

	public Collection<ResolverExtractor> getExtractors() {
		return extractors;
	}
}
