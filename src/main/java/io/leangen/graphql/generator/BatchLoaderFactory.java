package io.leangen.graphql.generator;

import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.execution.OperationExecutor;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static io.leangen.graphql.util.Utils.failedFuture;

public class BatchLoaderFactory {

    @SuppressWarnings("unchecked")
    public BatchLoaderWithContext<?, ?> createBatchLoader(OperationExecutor executor) {
        return (keys, env) -> {
            try {
                List<KeyContextsWrapper> split = splitByArguments(keys, env);
                if (split.size() == 1) {
                    return (CompletionStage<List<Object>>) executor.execute(keys, env);
                }
                return runInBatches(keys.size(), split, executor);
            } catch (Exception e) {
                return failedFuture(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public BatchLoaderWithContext<?, ?> createAsyncBatchLoader(OperationExecutor executor) {
        return (keys, env) -> CompletableFuture.supplyAsync(()-> {
            try {
                List<KeyContextsWrapper> split = splitByArguments(keys, env);
                if (split.size() == 1) {
                    return (List<Object>) executor.execute(keys, env);
                }
                return runInBatches(keys.size(), split, executor).get();
            } catch (CompletionException e) {
                throw e;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private List<KeyContextsWrapper> splitByArguments(List<Object> keys, BatchLoaderEnvironment env) {
        Map<Map<String, Object>, KeyContextsWrapper> byArgumentsMap = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            Object keyContext = env.getKeyContextsList().get(i);
            Map<String, Object> arguments = (keyContext instanceof DataFetchingEnvironment) ?
                    ((DataFetchingEnvironment) keyContext).getArguments() : null;
            KeyContextsWrapper kc = byArgumentsMap.computeIfAbsent(
                    Optional.ofNullable(arguments).orElse(Collections.emptyMap()), k -> new KeyContextsWrapper(env));
            kc.add(i, keys.get(0), keyContext);
        }
        return new ArrayList<>(byArgumentsMap.values());
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<List<Object>> runInBatches(final int totalSize, List<KeyContextsWrapper> keyContextsWrapper, OperationExecutor executor) throws Exception {
        CompletableFuture<List<Object>>[] futures = new CompletableFuture[keyContextsWrapper.size()];
        int i = 0;
        for (KeyContextsWrapper context : keyContextsWrapper) {
            Object result = executor.execute(context.keys, context.newBatchLoaderEnvironment());
            if (result instanceof List) {
                context.result = CompletableFuture.completedFuture((List<Object>) result);
            } else {
                context.result = (CompletableFuture<List<Object>>) result;
            }
            futures[i++] = context.result;
        }
        return CompletableFuture.allOf(futures)
                .thenCompose(v -> CompletableFuture.completedFuture(mergeResults(totalSize, keyContextsWrapper)));
    }

    private List<Object> mergeResults(int totalSize, List<KeyContextsWrapper> keyContextsWrapper) {
        try {
            Object[] result = new Object[totalSize];
            for (KeyContextsWrapper context : keyContextsWrapper) {
                List<Object> partResult = context.result.toCompletableFuture().get();
                for (int i = 0; i < context.indexes.size(); i++) {
                    result[context.indexes.get(i)] = partResult.get(i);
                }
            }
            return Arrays.asList(result);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class KeyContextsWrapper {
        final List<Object> keys = new ArrayList<>();
        final List<Object> keyContexts = new ArrayList<>();
        final List<Integer> indexes = new ArrayList<>();

        CompletableFuture<List<Object>> result;
        private final BatchLoaderEnvironment env;

        public KeyContextsWrapper(BatchLoaderEnvironment env) {
            this.env = env;
        }

        void add(Integer index, Object key, Object keyContext) {
            indexes.add(index);
            keys.add(key);
            keyContexts.add(keyContext);
        }

        BatchLoaderEnvironment newBatchLoaderEnvironment() {
            return BatchLoaderEnvironment.newBatchLoaderEnvironment()
                    .context(env.getContext())
                    .keyContexts(keys, keyContexts)
                    .build();
        }
    }
}
