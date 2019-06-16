package io.leangen.graphql.support;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLModifiedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.util.GraphQLUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GraphQLTypeAssertions {

    public static void assertNonNull(GraphQLType wrapperType, GraphQLType wrappedType) {
        assertEquals(GraphQLNonNull.class, wrapperType.getClass());
        assertEquals(((GraphQLNonNull) wrapperType).getWrappedType(), wrappedType);
    }

    public static void assertNonNull(GraphQLType wrapperType, Class<? extends GraphQLType> wrappedTypeClass) {
        assertEquals(GraphQLNonNull.class, wrapperType.getClass());
        assertEquals(((GraphQLNonNull) wrapperType).getWrappedType().getClass(), wrappedTypeClass);
    }

    public static void assertListOfNonNull(GraphQLType wrapperType, GraphQLType wrappedType) {
        assertEquals(wrapperType.getClass(), GraphQLList.class);
        assertEquals(((GraphQLList) wrapperType).getWrappedType().getClass(), GraphQLNonNull.class);
        assertEquals(((GraphQLNonNull) (((GraphQLList) wrapperType).getWrappedType())).getWrappedType(), wrappedType);
    }

    public static void assertListOfNonNull(GraphQLType wrapperType, Class<? extends GraphQLModifiedType> wrappedTypeClass) {
        assertEquals(wrapperType.getClass(), GraphQLList.class);
        assertEquals(((GraphQLList) wrapperType).getWrappedType().getClass(), GraphQLNonNull.class);
        assertEquals(((GraphQLNonNull) (((GraphQLList) wrapperType).getWrappedType())).getWrappedType().getClass(), wrappedTypeClass);
    }

    public static void assertListOf(GraphQLType wrapperType, GraphQLType wrappedType) {
        assertEquals(GraphQLList.class, wrapperType.getClass());
        assertEquals(wrappedType, ((GraphQLList) wrapperType).getWrappedType());
    }

    public static void assertListOf(GraphQLType wrapperType, Class<? extends GraphQLType> wrappedType) {
        assertEquals(GraphQLList.class, wrapperType.getClass());
        assertEquals(wrappedType, ((GraphQLList) wrapperType).getWrappedType().getClass());
    }

    public static void assertListOfRelayIds(GraphQLType type) {
        assertEquals(GraphQLList.class, type.getClass());
        assertTrue(GraphQLUtils.isRelayId(((GraphQLList) type).getWrappedType()));
    }
    
    public static void assertMapOf(GraphQLType mapType, Class<? extends GraphQLType> keyType, Class<? extends GraphQLType> valueType) {
        assertEquals(GraphQLList.class, mapType.getClass());
        GraphQLType entry = GraphQLUtils.unwrap(mapType);
        assertTrue(entry instanceof GraphQLObjectType);
        GraphQLOutputType key = ((GraphQLObjectType) entry).getFieldDefinition("key").getType();
        GraphQLOutputType value = ((GraphQLObjectType) entry).getFieldDefinition("value").getType();
        assertTrue(keyType.isAssignableFrom(key.getClass()));
        assertTrue(valueType.isAssignableFrom(value.getClass()));
    }

    public static void assertInputMapOf(GraphQLType mapType, Class<? extends GraphQLType> keyType, Class<? extends GraphQLType> valueType) {
        assertEquals(GraphQLList.class, mapType.getClass());
        GraphQLType entry = GraphQLUtils.unwrap(mapType);
        assertTrue(entry instanceof GraphQLInputObjectType);
        GraphQLInputType key = ((GraphQLInputObjectType) entry).getFieldDefinition("key").getType();
        GraphQLInputType value = ((GraphQLInputObjectType) entry).getFieldDefinition("value").getType();
        assertTrue(keyType.isAssignableFrom(key.getClass()));
        assertTrue(valueType.isAssignableFrom(value.getClass()));
    }

    public static void assertUnionOf(GraphQLType unionType, GraphQLType... members) {
        GraphQLUnionType union = (GraphQLUnionType) unionType;
        assertEquals(union.getTypes().size(), members.length);
        assertTrue(union.getTypes().containsAll(Arrays.asList(members)));
    }

    public static void assertArgumentsPresent(GraphQLFieldDefinition field, String... argumentNames) {
        Set<String> argNames = field.getArguments().stream().map(GraphQLArgument::getName).collect(Collectors.toSet());
        Arrays.stream(argumentNames).forEach(argName -> assertTrue(argNames.contains(argName)));
    }

    public static void assertFieldNamesEqual(GraphQLFieldsContainer fieldsContainer, String... fieldNames) {
        Set<String> discoveredNames = fieldsContainer.getFieldDefinitions().stream()
                .map(GraphQLFieldDefinition::getName).collect(Collectors.toSet());
        assertEquals(fieldNames.length, discoveredNames.size());
        Arrays.stream(fieldNames).forEach(fldName -> assertTrue(discoveredNames.contains(fldName)));
    }
}
