package io.leangen.graphql.util;

import graphql.relay.Relay;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import io.leangen.graphql.annotations.GraphQLId;

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

    public static boolean isRelayNodeInterface(GraphQLType node) {
        return node instanceof GraphQLInterfaceType
                && node.getName().equals(Relay.NODE)
                && ((GraphQLInterfaceType) node).getFieldDefinitions().size() == 1
                && ((GraphQLInterfaceType) node).getFieldDefinition("id") != null
                && isRelayId(((GraphQLInterfaceType) node).getFieldDefinition("id"));
    }

    public static GraphQLType unwrapNonNull(GraphQLType type) {
        if (type instanceof GraphQLNonNull) {
            return unwrapNonNull(((GraphQLNonNull) type).getWrappedType());
        }
        return type;
    }
}
