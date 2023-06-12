package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.execution.InvocationContext;
import io.leangen.graphql.execution.ResolverInterceptor;
import io.leangen.graphql.execution.ResolverInterceptorFactory;
import io.leangen.graphql.execution.ResolverInterceptorFactoryParams;
import io.leangen.graphql.util.Utils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class BatchLoaderAdapterFactory implements ResolverInterceptorFactory {

    private final BatchLoaderAdapter adapter;

    public BatchLoaderAdapterFactory(Executor executor) {
        this.adapter = new BatchLoaderAdapter(executor);
    }

    @Override
    public List<ResolverInterceptor> getInterceptors(ResolverInterceptorFactoryParams params) {
        return Collections.emptyList();
    }

    @Override
    public List<ResolverInterceptor> getOuterInterceptors(ResolverInterceptorFactoryParams params) {
        if (params.getResolver().isBatched() && !params.getResolver().isAsync()) {
            return Collections.singletonList(adapter);
        }
        return Collections.emptyList();
    }

    public static class BatchLoaderAdapter implements ResolverInterceptor {

        private final Executor executor;

        public BatchLoaderAdapter(Executor executor) {
            this.executor = executor;
        }

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) {
            if (executor != null) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return continuation.proceed(context);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);
            }
            try {
                return CompletableFuture.completedFuture(continuation.proceed(context));
            } catch (Exception e) {
                return Utils.failedFuture(e);
            }
        }
    }
}
