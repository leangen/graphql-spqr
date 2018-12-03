package io.leangen.graphql.generator.mapping;

import graphql.language.OperationDefinition;

public class ConverterSupportParams {

    private final OperationDefinition.Operation operationType;

    public ConverterSupportParams(OperationDefinition.Operation operationType) {
        this.operationType = operationType;
    }

    public OperationDefinition.Operation getOperationType() {
        return operationType;
    }
}
