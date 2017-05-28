package io.leangen.graphql.execution.instrumentation;

import java.util.List;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.execution.instrumentation.parameters.DataFetchParameters;
import graphql.execution.instrumentation.parameters.ExecutionParameters;
import graphql.execution.instrumentation.parameters.FieldFetchParameters;
import graphql.execution.instrumentation.parameters.FieldParameters;
import graphql.execution.instrumentation.parameters.ValidationParameters;
import graphql.language.Document;
import graphql.validation.ValidationError;

public class ComplexityAnalysisInstrumentation implements Instrumentation {

    private final ComplexityFunction complexityFunction;
    private final int maximumComplexity;

    public ComplexityAnalysisInstrumentation(ComplexityFunction complexityFunction, int maximumComplexity) {
        this.complexityFunction = complexityFunction;
        this.maximumComplexity = maximumComplexity;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(ExecutionParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginExecution(parameters);
    }

    @Override
    public InstrumentationContext<Document> beginParse(ExecutionParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginParse(parameters);
    }

    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(ValidationParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginValidation(parameters);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginDataFetch(DataFetchParameters parameters) {
        QueryTreeNode root = new ComplexityAnalyzer(complexityFunction, maximumComplexity).collectFields(parameters.getExecutionContext());
        return NoOpInstrumentation.INSTANCE.beginDataFetch(parameters);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(FieldParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginField(parameters);
    }

    @Override
    public InstrumentationContext<Object> beginFieldFetch(FieldFetchParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginFieldFetch(parameters);
    }
}
