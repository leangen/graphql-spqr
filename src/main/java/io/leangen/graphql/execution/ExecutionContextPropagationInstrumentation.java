package io.leangen.graphql.execution;

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

/**
 * Created by bojan.tomic on 4/14/17.
 */
public class ExecutionContextPropagationInstrumentation implements Instrumentation {
    
    public static final String EXECUTION_CONTEXT_KEY = "\n\t\t\n\t\t\n\ue000\ue001\ue002EXECUTION_CONTEXT\n\t\t\t\t\n"; 
    
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
        return NoOpInstrumentation.INSTANCE.beginDataFetch(parameters);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(FieldParameters parameters) {
        return NoOpInstrumentation.INSTANCE.beginField(parameters);
    }

    @Override
    public InstrumentationContext<Object> beginFieldFetch(FieldFetchParameters parameters) {
        parameters.getEnvironment().getArguments().put(EXECUTION_CONTEXT_KEY, parameters.getExecutionContext());
        return NoOpInstrumentation.INSTANCE.beginFieldFetch(parameters);
    }
}
