package io.leangen.graphql.util;

import graphql.Scalars;
import graphql.language.Field;
import graphql.relay.Relay;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLModifiedType;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLType;
import io.leangen.graphql.annotations.GraphQLId;

public class GraphQLUtils {

    public static final String BASIC_INTROSPECTION_QUERY = "{ __schema { queryType { name fields { name type { name kind ofType { name kind fields { name } }}}}}}";
    public static final String FULL_INTROSPECTION_QUERY = "query IntrospectionQuery { __schema { queryType { name } mutationType { name } types { ...FullType } directives { name description args { ...InputValue } onOperation onFragment onField } } } fragment FullType on __Type { kind name description fields(includeDeprecated: true) { name description args { ...InputValue } type { ...TypeRef } isDeprecated deprecationReason } inputFields { ...InputValue } interfaces { ...TypeRef } enumValues(includeDeprecated: true) { name description isDeprecated deprecationReason } possibleTypes { ...TypeRef } } fragment InputValue on __InputValue { name description type { ...TypeRef } defaultValue } fragment TypeRef on __Type { kind name ofType { kind name ofType { kind name ofType { kind name } } } }";

    public static final String CLIENT_MUTATION_ID = "clientMutationId";
    public static final String NODE = "node";
    private static final String EDGES = "edges";
    private static final String PAGE_INFO = "pageInfo";
    private static final String CONNECTION = "Connection";

    public static boolean isRelayId(GraphQLFieldDefinition field) {
        return field.getName().equals(GraphQLId.RELAY_ID_FIELD_NAME) && isRelayId(field.getType());
    }

    public static boolean isRelayId(GraphQLArgument argument) {
        return argument.getName().equals(GraphQLId.RELAY_ID_FIELD_NAME) && isRelayId(argument.getType());
    }

    public static boolean isRelayId(GraphQLType type) {
        return type instanceof GraphQLNonNull && ((GraphQLNonNull) type).getWrappedType().equals(Scalars.GraphQLID);
    }

    public static boolean isRelayNodeInterface(GraphQLType node) {
        if (!(node instanceof GraphQLInterfaceType)) {
            return false;
        }
        GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) node;
        return interfaceType.getName().equals(Relay.NODE)
                && interfaceType.getFieldDefinitions().size() == 1
                && interfaceType.getFieldDefinition(GraphQLId.RELAY_ID_FIELD_NAME) != null
                && isRelayId(interfaceType.getFieldDefinition(GraphQLId.RELAY_ID_FIELD_NAME));
    }

    public static boolean isRelayConnectionType(GraphQLType type) {
        if (!(type instanceof GraphQLObjectType)) {
            return false;
        }
        GraphQLObjectType objectType = (GraphQLObjectType) type;
        return !objectType.getName().equals(CONNECTION) && objectType.getName().endsWith(CONNECTION)
                && objectType.getFieldDefinition(EDGES) != null && objectType.getFieldDefinition(PAGE_INFO) != null;
    }

    public static boolean isRelayConnectionField(GraphQLFieldDefinition fieldDefinition) {
        return fieldDefinition.getName().equals(EDGES) || fieldDefinition.getName().equals(
                PAGE_INFO);
    }

    public static boolean isRelayEdgeField(GraphQLFieldDefinition fieldDefinition) {
        return fieldDefinition.getName().equals(NODE) || fieldDefinition.getName().equals("cursor");
    }

    public static boolean isIntrospectionType(GraphQLType type) {
        return type instanceof GraphQLNamedType && isIntrospection(name(type));
    }

    public static boolean isIntrospectionField(Field field) {
        return isIntrospection(field.getName());
    }

    public static GraphQLType unwrapNonNull(GraphQLType type) {
        while (type instanceof GraphQLNonNull) {
            type = ((GraphQLNonNull) type).getWrappedType();
        }
        return type;
    }

    public static GraphQLNamedType unwrap(GraphQLType type) {
        while (type instanceof GraphQLModifiedType) {
            type = ((GraphQLModifiedType) type).getWrappedType();
        }
        return (GraphQLNamedType) type;
    }

    public static String name(GraphQLSchemaElement element) {
        return ((GraphQLNamedSchemaElement) element).getName();
    }

    private static boolean isIntrospection(String name) {
        return name != null && name.startsWith("__");
    }
}
