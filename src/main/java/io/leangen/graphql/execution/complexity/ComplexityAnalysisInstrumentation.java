package io.leangen.graphql.execution.complexity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationDataFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldParameters;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.AstPrinter;
import graphql.language.Document;
import graphql.validation.ValidationError;

public class ComplexityAnalysisInstrumentation implements Instrumentation {

    private final ComplexityFunction complexityFunction;
    private final int maximumComplexity;

    private static final Logger log = LoggerFactory.getLogger(ComplexityAnalysisInstrumentation.class);
    
    public ComplexityAnalysisInstrumentation(ComplexityFunction complexityFunction, int maximumComplexity) {
        this.complexityFunction = complexityFunction;
        this.maximumComplexity = maximumComplexity;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginExecution(parameters);
    }

    @Override
    public InstrumentationContext<Document> beginParse(InstrumentationExecutionParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginParse(parameters);
    }

    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(InstrumentationValidationParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginValidation(parameters);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginDataFetch(InstrumentationDataFetchParameters parameters) {
        ResolvedField root = new ComplexityAnalyzer(complexityFunction, maximumComplexity).collectFields(parameters.getExecutionContext());
        if (log.isDebugEnabled()) {
            log.debug("Operation {} has total complexity of {}",
                    AstPrinter.printAst(parameters.getExecutionContext().getOperationDefinition().getSelectionSet().getSelections().get(0)),
                    root.getComplexityScore());
        }
        log.info("Total operation complexity: {}", root.getComplexityScore());
        return NoOpInstrumentation.INSTANCE.beginDataFetch(parameters);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(InstrumentationFieldParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginField(parameters);
    }

    @Override
    public InstrumentationContext<Object> beginFieldFetch(InstrumentationFieldFetchParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginFieldFetch(parameters);
    }

    @Override
    public InstrumentationContext<CompletableFuture<ExecutionResult>> beginExecutionStrategy(
            InstrumentationExecutionStrategyParameters parameters) {
        return (result, exception) -> {};
    }
}
