package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;

public interface SchemaTransformer {

    default GraphQLFieldDefinition transformField(GraphQLFieldDefinition field, Operation operation, OperationMapper operationMapper, BuildContext buildContext) {
        return field;
    }

    default GraphQLInputObjectField transformInputField(GraphQLInputObjectField field, InputField inputField, OperationMapper operationMapper, BuildContext buildContext) {
        return field;
    }

    default GraphQLArgument transformArgument(GraphQLArgument argument, OperationArgument operationArgument, OperationMapper operationMapper, BuildContext buildContext) {
        return argument;
    }
}
