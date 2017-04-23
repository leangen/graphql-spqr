package io.leangen.graphql.util;

import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import io.leangen.graphql.annotations.GraphQLId;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphQLUtils {

    public static final String BASIC_INTROSPECTION_QUERY = "{ __schema { queryType { name fields { name type { name kind ofType { name kind fields { name } }}}}}}";

    public static final String FULL_INTROSPECTION_QUERY = "query IntrospectionQuery { __schema { queryType { name } mutationType { name } types { ...FullType } directives { name description args { ...InputValue } onOperation onFragment onField } } } fragment FullType on __Type { kind name description fields(includeDeprecated: true) { name description args { ...InputValue } type { ...TypeRef } isDeprecated deprecationReason } inputFields { ...InputValue } interfaces { ...TypeRef } enumValues(includeDeprecated: true) { name description isDeprecated deprecationReason } possibleTypes { ...TypeRef } } fragment InputValue on __InputValue { name description type { ...TypeRef } defaultValue } fragment TypeRef on __Type { kind name ofType { kind name ofType { kind name ofType { kind name } } } }";

    public static boolean isRelayId(GraphQLFieldDefinition field) {
        return field.getName().equals(GraphQLId.RELAY_ID_FIELD_NAME) && isRelayId(field.getType());
    }
    
    public static boolean isRelayId(GraphQLArgument argument) {
        return argument.getName().equals(GraphQLId.RELAY_ID_FIELD_NAME) && isRelayId(argument.getType());
    }

    public static boolean isRelayId(GraphQLType type) {
        return type.equals(Scalars.RelayId);
    }
    
    public static GraphQLType unwrapNonNull(GraphQLType type) {
        if (type instanceof GraphQLNonNull) {
            return unwrapNonNull(((GraphQLNonNull) type).getWrappedType());
        }
        return type;
    }

    public GraphQLFieldDefinition mutationWithClientMutationId(String name, String fieldName,
                                                               List<GraphQLInputObjectField> inputFields,
                                                               List<GraphQLFieldDefinition> outputFields,
                                                               DataFetcher dataFetcher) {
        GraphQLInputObjectType inputObjectType = newInputObject()
                .name(name + "Input")
                .field(newInputObjectField()
                        .name("clientMutationId")
                        .type(new GraphQLNonNull(GraphQLString)))
                .fields(inputFields)
                .build();
        GraphQLObjectType outputType = newObject()
                .name(name + "Payload")
                .field(newFieldDefinition()
                        .name("clientMutationId")
                        .type(new GraphQLNonNull(GraphQLString)))
                .fields(outputFields)
                .build();

        return newFieldDefinition()
                .name(fieldName)
                .type(outputType)
                .argument(newArgument()
                        .name("input")
                        .type(new GraphQLNonNull(inputObjectType)))
                .dataFetcher(dataFetcher)
                .build();
    }
}
