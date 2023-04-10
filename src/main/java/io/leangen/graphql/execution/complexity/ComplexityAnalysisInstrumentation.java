package io.leangen.graphql.execution.complexity;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.AstPrinter;
import io.leangen.graphql.generator.TypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexityAnalysisInstrumentation extends SimplePerformantInstrumentation {

    private final ComplexityAnalyzer analyzer;

    private static final Logger log = LoggerFactory.getLogger(ComplexityAnalysisInstrumentation.class);

    public ComplexityAnalysisInstrumentation(int maxComplexity, ComplexityFunction complexityFunction, TypeRegistry typeRegistry) {
        this.analyzer = new ComplexityAnalyzer(maxComplexity, complexityFunction, typeRegistry);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {
        int totalComplexity = analyzer.complexity(parameters.getExecutionContext());
        if (log.isDebugEnabled()) {
            log.debug("Operation {} has total complexity of {}",
                    AstPrinter.printAst(parameters.getExecutionContext().getOperationDefinition().getSelectionSet().getSelections().get(0)),
                    totalComplexity);
        }
        log.info("Total operation complexity: {}", totalComplexity);
        return super.beginExecuteOperation(parameters, state);
    }
}
