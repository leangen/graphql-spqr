package io.leangen.graphql.generator.types;

import graphql.schema.GraphQLFieldDefinition;
import io.leangen.graphql.metadata.Operation;

public class MappedGraphQLFieldDefinition extends GraphQLFieldDefinition {
    
    private final Operation operation;
    
    public MappedGraphQLFieldDefinition(GraphQLFieldDefinition fieldDefinition, Operation operation) {
        super(fieldDefinition.getName(), fieldDefinition.getDescription(), fieldDefinition.getType(),
                fieldDefinition.getDataFetcher(), fieldDefinition.getArguments(), fieldDefinition.getDeprecationReason());
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }
}
