package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.execution.complexity.ComplexityAnalysisInstrumentation;
import io.leangen.graphql.execution.complexity.JavaScriptEvaluator;
import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.util.ContextUtils;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationState.EMPTY_DATALOADER_REGISTRY;

/**
 * Wrapper around GraphQL builder that allows easy instrumentation chaining, limiting query complexity and context wrapping
 */
public class GraphQLRuntime {

    public static Builder newGraphQL(GraphQLSchema graphQLSchema) {
        return new Builder(graphQLSchema);
    }

    public static Builder newGraphQL(ExecutableSchema graphQLSchema) {
        return new Builder(graphQLSchema.getSchema())
                .typeRegistry(graphQLSchema.getTypeRegistry())
                .batchLoaders(graphQLSchema.getBatchLoaders());
    }

    public static class Builder extends GraphQL.Builder {

        private final List<Instrumentation> instrumentations;
        private final Map<String, BatchLoaderWithContext<?, ?>> batchLoaders;
        private final Map<String, DataLoaderOptions> dataLoaderOptions;
        private TypeRegistry typeRegistry;

        private Builder(GraphQLSchema graphQLSchema) {
            super(graphQLSchema);
            this.instrumentations = new ArrayList<>();
            this.batchLoaders = new HashMap<>();
            this.dataLoaderOptions = new HashMap<>();
            this.typeRegistry = new TypeRegistry(Collections.emptyMap());
        }

        @Override
        public Builder instrumentation(Instrumentation instrumentation) {
            this.instrumentations.add(instrumentation);
            return this;
        }

        public Builder maximumQueryComplexity(int limit) {
            instrumentations.add(new ComplexityAnalysisInstrumentation(new JavaScriptEvaluator(), limit, typeRegistry));
            return this;
        }

        public Builder batchLoader(String loaderName, BatchLoaderWithContext<?, ?> loader) {
            batchLoaders.put(loaderName, loader);
            return this;
        }

        public Builder batchLoaders(Map<String, BatchLoaderWithContext<?, ?>> loaders) {
            batchLoaders.putAll(loaders);
            return this;
        }

        public Builder dataLoaderOptions(DataLoaderOptions options) {
            return dataLoaderOptions(null, options);
        }

        public Builder dataLoaderOptions(String loaderName, DataLoaderOptions options) {
            this.dataLoaderOptions.put(loaderName, options);
            return this;
        }

        public Builder typeRegistry(TypeRegistry typeRegistry) {
            this.typeRegistry = Objects.requireNonNull(typeRegistry);
            return this;
        }

        @Override
        public GraphQL build() {
            List<Instrumentation> instrumentations = new ArrayList<>();
            instrumentations.add(new InputTransformer(batchLoaders, dataLoaderOptions));
            if (!batchLoaders.isEmpty() && this.instrumentations.stream().noneMatch(inst -> inst instanceof DataLoaderDispatcherInstrumentation)) {
                instrumentations.add(new DataLoaderDispatcherInstrumentation());
            }
            instrumentations.addAll(this.instrumentations);
            if (instrumentations.size() == 1) {
                super.instrumentation(instrumentations.get(0));
            } else {
                super.instrumentation(new ChainedInstrumentation(instrumentations));
            }
            return super.build();
        }
    }

    static class InputTransformer extends SimpleInstrumentation {

        private final Map<String, BatchLoaderWithContext<?, ?>> batchLoaders;
        private final Map<String, DataLoaderOptions> dataLoaderOptions;

        InputTransformer(Map<String, BatchLoaderWithContext<?, ?>> batchLoaders,
                         Map<String, DataLoaderOptions> dataLoaderOptions) {
            this.batchLoaders = batchLoaders;
            this.dataLoaderOptions = dataLoaderOptions;
        }

        @Override
        public ExecutionInput instrumentExecutionInput(ExecutionInput executionInput,
                                                       InstrumentationExecutionParameters parameters,
                                                       InstrumentationState state) {

            ExecutionInput input = ContextUtils.wrapContext(executionInput);
            //Init DataLoaders automatically
            DataLoaderOptions defaultOptions = dataLoaderOptions.getOrDefault(null,
                    DataLoaderOptions.newOptions()
                            .setBatchLoaderContextProvider(input::getContext));
            DataLoaderRegistry registry = input.getDataLoaderRegistry() != EMPTY_DATALOADER_REGISTRY
                    ? input.getDataLoaderRegistry()
                    : new DataLoaderRegistry();

            if (!batchLoaders.isEmpty()) {
                batchLoaders.forEach((loaderName, batchLoader) -> {
                    if (registry.getKeys().contains(loaderName)) {
                        throw new ConfigurationException("DataLoader name collision: " + loaderName + " is registered both manually and automatically.");
                    }
                    DataLoaderOptions options = dataLoaderOptions.getOrDefault(loaderName, defaultOptions);
                    DataLoader<?, ?> dataLoader = DataLoaderFactory.newDataLoader(batchLoader, options);
                    registry.register(loaderName, dataLoader);
                });
            }
            return input.transform(in -> in.dataLoaderRegistry(registry));
        }
    }
}
