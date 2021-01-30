package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.execution.complexity.ComplexityAnalysisInstrumentation;
import io.leangen.graphql.execution.complexity.JavaScriptEvaluator;
import io.leangen.graphql.util.ContextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around GraphQL builder that allows easy instrumentation chaining, limiting query complexity and context wrapping
 */
public class GraphQLRuntime {

    public static Builder newGraphQL(GraphQLSchema graphQLSchema) {
        return new Builder(graphQLSchema);
    }

    public static class Builder extends GraphQL.Builder {

        private final List<Instrumentation> instrumentations;

        private Builder(GraphQLSchema graphQLSchema) {
            super(graphQLSchema);
            List<Instrumentation> defaultInstrumentations = new ArrayList<>();
            defaultInstrumentations.add(new ContextWrappingInstrumentation());
            this.instrumentations = defaultInstrumentations;
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
        public GraphQL build() {
            if (instrumentations.size() == 1) {
                super.instrumentation(instrumentations.get(0));
            } else if (!instrumentations.isEmpty()) {
                super.instrumentation(new ChainedInstrumentation(instrumentations));
            }
            return super.build();
        }
    }

    public static class ContextWrappingInstrumentation extends SimpleInstrumentation {

        @Override
        public ExecutionInput instrumentExecutionInput(ExecutionInput executionInput, InstrumentationExecutionParameters parameters) {
            return ContextUtils.wrapContext(executionInput);
        }
    }
}
