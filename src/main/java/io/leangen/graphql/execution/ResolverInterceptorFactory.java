package io.leangen.graphql.execution;

import io.leangen.graphql.metadata.Resolver;

import java.util.List;

public interface ResolverInterceptorFactory {

    List<ResolverInterceptor> getInterceptors(Resolver resolver);
}
