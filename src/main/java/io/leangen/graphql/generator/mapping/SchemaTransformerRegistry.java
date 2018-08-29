package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.OperationArgument;

import java.util.Collections;
import java.util.List;

public class SchemaTransformerRegistry {

    private final List<SchemaTransformer> transformers;

    public SchemaTransformerRegistry(List<SchemaTransformer> transformers) {
        this.transformers = Collections.unmodifiableList(transformers);
    }

    public GraphQLFieldDefinition transform(GraphQLFieldDefinition field, Operation operation, OperationMapper operationMapper, BuildContext buildContext) {
        for (SchemaTransformer transformer : transformers) {
            field = transformer.transformField(field, operation, operationMapper, buildContext);
        }
        return field;
    }

    public GraphQLInputObjectField transform(GraphQLInputObjectField field, InputField inputField, OperationMapper operationMapper, BuildContext buildContext) {
        for (SchemaTransformer transformer : transformers) {
            field = transformer.transformInputField(field, inputField, operationMapper, buildContext);
        }
        return field;
    }

    public GraphQLArgument transform(GraphQLArgument argument, OperationArgument operationArgument, OperationMapper operationMapper, BuildContext buildContext) {
        for (SchemaTransformer transformer : transformers) {
            argument = transformer.transformArgument(argument, operationArgument, operationMapper, buildContext);
        }
        return argument;
    }
}
