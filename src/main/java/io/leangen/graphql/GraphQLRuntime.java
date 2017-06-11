package io.leangen.graphql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.DataFetchParameters;
import graphql.execution.instrumentation.parameters.ExecutionParameters;
import graphql.execution.instrumentation.parameters.FieldFetchParameters;
import graphql.execution.instrumentation.parameters.FieldParameters;
import graphql.execution.instrumentation.parameters.ValidationParameters;
import graphql.language.Document;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import io.leangen.graphql.execution.ContextWrapper;
import io.leangen.graphql.execution.instrumentation.ComplexityAnalysisInstrumentation;
import io.leangen.graphql.execution.instrumentation.JavaScriptEvaluator;

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

        public Builder(GraphQLSchema graphQLSchema) {
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
        public InstrumentationContext<ExecutionResult> beginExecution(ExecutionParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginExecution(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<Document> beginParse(ExecutionParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginParse(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<List<ValidationError>> beginValidation(ValidationParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginValidation(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<ExecutionResult> beginDataFetch(DataFetchParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginDataFetch(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<ExecutionResult> beginField(FieldParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginField(parameters))
                    .collect(Collectors.toList()));
        }

        @Override
        public InstrumentationContext<Object> beginFieldFetch(FieldFetchParameters parameters) {
            return new InstrumentationContextChain<>(instrumentations.stream()
                    .map(instrumentation -> instrumentation.beginFieldFetch(parameters))
                    .collect(Collectors.toList()));
        }
    }

    public static class InstrumentationContextChain<T> implements InstrumentationContext<T> {

        private final List<InstrumentationContext<T>> contexts;

        InstrumentationContextChain(List<InstrumentationContext<T>> contexts) {
            this.contexts = Collections.unmodifiableList(contexts);
        }

        @Override
        public void onEnd(T result) {
            contexts.forEach(context -> context.onEnd(result));
        }

        @Override
        public void onEnd(Exception e) {
            contexts.forEach(context -> context.onEnd(e));
        }
    }

}
