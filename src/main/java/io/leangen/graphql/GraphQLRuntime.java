package io.leangen.graphql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationDataFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.Document;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import io.leangen.graphql.execution.ContextWrapper;
import io.leangen.graphql.execution.complexity.ComplexityAnalysisInstrumentation;
import io.leangen.graphql.execution.complexity.JavaScriptEvaluator;

/**
 * Wrapper around GraphQL that allows instrumentation chaining and limiting query complexity
 */
public class GraphQLRuntime extends GraphQL {

    private final GraphQL delegate;

    @SuppressWarnings("deprecation")
    private GraphQLRuntime(GraphQL delegate, GraphQLSchema schema) {
        super(schema);
        this.delegate = delegate;
    }

    @Override
    public ExecutionResult execute(String requestString, String operationName, Object context, Map<String, Object> arguments) {
        return delegate.execute(requestString, operationName, new ContextWrapper(context), arguments);
    }

    public static Builder newGraphQL(GraphQLSchema graphQLSchema) {
        return new Builder(graphQLSchema);
    }

    public static class Builder extends GraphQL.Builder {

        private GraphQLSchema graphQLSchema;
        private List<Instrumentation> instrumentations;

        private Builder(GraphQLSchema graphQLSchema) {
            super(graphQLSchema);
            this.graphQLSchema = graphQLSchema;
            this.instrumentations = new ArrayList<>();
        }

        @Override
        public Builder instrumentation(Instrumentation instrumentation) {
            this.instrumentations.add(instrumentation);
            return this;
        }

        public Builder maximumQueryComplexity(int limit) {
            instrumentations.add(new ComplexityAnalysisInstrumentation(new JavaScriptEvaluator(), limit));
            return this;
        }

        @Override
        public GraphQLRuntime build() {
            if (instrumentations.size() == 1) {
                super.instrumentation(instrumentations.get(0));
            } else if (!instrumentations.isEmpty()) {
                super.instrumentation(new InstrumentationChain(instrumentations));
            }
            return new GraphQLRuntime(super.build(), graphQLSchema);
        }
    }

    public static class InstrumentationChain implements Instrumentation {

        private final List<Instrumentation> instrumentations;

        public InstrumentationChain(List<Instrumentation> instrumentations) {
            this.instrumentations = Collections.unmodifiableList(instrumentations);
        }

        @Override
        public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginExecution(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<Document> beginParse(InstrumentationExecutionParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginParse(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<List<ValidationError>> beginValidation(InstrumentationValidationParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginValidation(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<ExecutionResult> beginDataFetch(InstrumentationDataFetchParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginDataFetch(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<ExecutionResult> beginField(InstrumentationFieldParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginField(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<Object> beginFieldFetch(InstrumentationFieldFetchParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginFieldFetch(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<CompletableFuture<ExecutionResult>> beginExecutionStrategy(
                InstrumentationExecutionStrategyParameters parameters) {
            return (result, exception) -> {};
        }
    }

    public static class InstrumentationContextChain<T> implements InstrumentationContext<T> {

        private final List<InstrumentationContext<T>> contexts;

        InstrumentationContextChain(List<InstrumentationContext<T>> contexts) {
            this.contexts = Collections.unmodifiableList(contexts);
        }

        @Override
        public void onEnd(T result, Throwable exception) {
            contexts.forEach(context -> context.onEnd(result, exception));
        }

    }

}
