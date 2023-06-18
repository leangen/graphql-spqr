package io.leangen.graphql.generator;

import graphql.schema.DataFetchingEnvironment;
import io.leangen.graphql.execution.OperationExecutor;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.leangen.graphql.util.Utils.failedFuture;

public class BatchLoaderFactory {

    @SuppressWarnings("unchecked")
    public BatchLoaderWithContext<?, ?> createBatchLoader(OperationExecutor executor) {
        if (executor.getOperation().getArguments().isEmpty()) {
            return (keys, env) -> {
                try {
                    return (CompletionStage<List<Object>>) executor.execute(keys, Collections.emptyMap(), env);
                } catch (Exception e) {
                    return failedFuture(e);
                }
            };
        }

        return (keys, env) -> {
            try {
                List<BatchSubTask> subTasks = split(keys, env);
                return runSubTasks(keys.size(), subTasks, executor);
            } catch (Exception e) {
                return failedFuture(e);
            }
        };
    }

    private List<BatchSubTask> split(List<Object> keys, BatchLoaderEnvironment env) {
        Map<Map<String, Object>, BatchSubTask> byArgumentsMap = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            Object keyContext = env.getKeyContextsList().get(i);
            Map<String, Object> arguments = (keyContext instanceof DataFetchingEnvironment) ?
                    ((DataFetchingEnvironment) keyContext).getArguments() : null;
            BatchSubTask kc = byArgumentsMap.computeIfAbsent(arguments, args -> new BatchSubTask(args, env));
            kc.add(i, keys.get(i), keyContext);
        }
        return new ArrayList<>(byArgumentsMap.values());
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<List<Object>> runSubTasks(final int totalSize, List<BatchSubTask> tasks, OperationExecutor executor) throws Exception {
        CompletableFuture<List<Object>>[] futures = new CompletableFuture[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            BatchSubTask task = tasks.get(i);
            task.result = ((CompletionStage<List<Object>>) executor.execute(task.keys, task.arguments, task.newBatchLoaderEnvironment())).toCompletableFuture();
            futures[i] = task.result;
        }

        return CompletableFuture.allOf(futures).thenApply(__ -> mergeResults(totalSize, tasks));
    }

    private List<Object> mergeResults(int totalSize, List<BatchSubTask> tasks) {
        Object[] result = new Object[totalSize];
        for (BatchSubTask task : tasks) {
            List<Object> partResult = task.result.join();
            for (int i = 0; i < task.indexes.size(); i++) {
                result[task.indexes.get(i)] = partResult.get(i);
            }
        }
        return Arrays.asList(result);
    }

    private static class BatchSubTask {
        final List<Object> keys = new ArrayList<>();
        final List<Object> keyContexts = new ArrayList<>();
        final List<Integer> indexes = new ArrayList<>();
        final Map<String, Object> arguments;

        CompletableFuture<List<Object>> result;
        private final BatchLoaderEnvironment env;

        public BatchSubTask(Map<String, Object> arguments, BatchLoaderEnvironment env) {
            this.arguments = arguments;
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
