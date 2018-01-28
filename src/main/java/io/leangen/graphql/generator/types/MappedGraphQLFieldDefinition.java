package io.leangen.graphql.generator.types;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactories;
import graphql.schema.DataFetcherFactory;
import graphql.schema.GraphQLFieldDefinition;
import io.leangen.graphql.metadata.Operation;

public class MappedGraphQLFieldDefinition extends GraphQLFieldDefinition {
    
    private final Operation operation;
    
    public MappedGraphQLFieldDefinition(GraphQLFieldDefinition fieldDefinition, Operation operation) {
        super(fieldDefinition.getName(), fieldDefinition.getDescription(), fieldDefinition.getType(),
                toFactory(fieldDefinition.getDataFetcher()), fieldDefinition.getArguments(),
                fieldDefinition.getDeprecationReason(), fieldDefinition.getDefinition());
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }

    private static DataFetcherFactory toFactory(DataFetcher<?> dataFetcher) {
        return DataFetcherFactories.useDataFetcher(dataFetcher);
    }
}
