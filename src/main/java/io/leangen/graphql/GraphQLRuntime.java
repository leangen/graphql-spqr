package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.dataloader.EmptyDataLoaderRegistryInstance;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.execution.complexity.ComplexityAnalysisInstrumentation;
import io.leangen.graphql.execution.complexity.ComplexityFunction;
import io.leangen.graphql.execution.complexity.SimpleComplexityFunction;
import io.leangen.graphql.generator.TypeRegistry;
import io.leangen.graphql.util.ContextUtils;
import org.dataloader.*;

import java.util.*;


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
            return maximumQueryComplexity(limit, new SimpleComplexityFunction());
        }

        public Builder maximumQueryComplexity(int limit, ComplexityFunction complexityFunction) {
            instrumentations.add(new ComplexityAnalysisInstrumentation(limit, complexityFunction, typeRegistry));
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
            instrumentations.addAll(this.instrumentations);
            if (instrumentations.size() == 1) {
                super.instrumentation(instrumentations.get(0));
            } else {
                super.instrumentation(new ChainedInstrumentation(instrumentations));
            }
            return super.build();
        }
    }

    static class InputTransformer extends SimplePerformantInstrumentation {

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
                            .setBatchLoaderContextProvider(input::getContext)
                            .setCacheKeyFunction(new CacheKeyFunction()));
            DataLoaderRegistry registry = input.getDataLoaderRegistry() != EmptyDataLoaderRegistryInstance.EMPTY_DATALOADER_REGISTRY
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

    public static class CacheKeyFunction implements CacheKey<Object> {

        @Override
        public Object getKey(Object input) {
            return input;
        }

        @Override
        public Object getKeyWithContext(Object input, Object context) {
            if (!(context instanceof DataFetchingEnvironment)) {
                return input;
            }
            return new Key(input, ((DataFetchingEnvironment) context).getArguments());
        }
    }

    public static class Key {
        private final Object key;
        private final Map<String, Object> arguments;

        public Key(Object key, Map<String, Object> arguments) {
            this.key = Objects.requireNonNull(key);
            this.arguments = Objects.requireNonNull(arguments);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key that = (Key) o;

            return key.equals(that.key) && arguments.equals(that.arguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, arguments);
        }
    }
}
