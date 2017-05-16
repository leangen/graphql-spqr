package io.leangen.graphql.generator.types;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.SchemaUtil;

/**
 * Created by bojan.tomic on 4/17/17.
 */
public class MeasuredGraphQLFieldDefinition extends GraphQLFieldDefinition {
    
    private final ComplexityFunction complexityFunction;
    private static final SchemaUtil schemaUtil = new SchemaUtil();

    public MeasuredGraphQLFieldDefinition(GraphQLFieldDefinition fieldDefinition) {
        this(fieldDefinition, schemaUtil.isLeafType(fieldDefinition.getType()) ? (arguments, childScore) -> 1d : (arguments, childScore) -> 1 + childScore);
    }
    
    public MeasuredGraphQLFieldDefinition(GraphQLFieldDefinition fieldDefinition, ComplexityFunction complexityFunction) {
        super(fieldDefinition.getName(), fieldDefinition.getDescription(), fieldDefinition.getType(), fieldDefinition.getDataFetcher(), fieldDefinition.getArguments(), fieldDefinition.getDeprecationReason(), fieldDefinition.getDefinition());
        this.complexityFunction = complexityFunction;
    }

    public ComplexityFunction getComplexityFunction() {
        return complexityFunction;
    }
}
