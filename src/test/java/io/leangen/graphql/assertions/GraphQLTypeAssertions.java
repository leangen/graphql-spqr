package io.leangen.graphql.assertions;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLModifiedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.util.GraphQLUtils;

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
        GraphQLType entry = ((GraphQLList) mapType).getWrappedType();
        assertTrue(entry instanceof GraphQLObjectType);
        GraphQLOutputType key = ((GraphQLObjectType) entry).getFieldDefinition("key").getType();
        GraphQLOutputType value = ((GraphQLObjectType) entry).getFieldDefinition("value").getType();
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
}
