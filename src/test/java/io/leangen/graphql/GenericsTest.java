package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.*;
import io.leangen.gentyref8.TypeToken;
import io.leangen.graphql.annotations.NonNull;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.domain.GenericItemRepo;
import org.junit.Test;

import java.lang.reflect.AnnotatedType;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by bojan.tomic on 7/25/16.
 */
public class GenericsTest {

    // IMPORTANT! All type declarations have to stay outside of tests (can not be inlined)
    // as the annotation parser treats them differently and discards the annotations otherwise
    public static final AnnotatedType nonNullString = new TypeToken<GenericItemRepo<@NonNull String>>() {
    }.getAnnotatedType();
    public static final AnnotatedType dateId = new TypeToken<GenericItemRepo<@RelayId Date>>() {
    }.getAnnotatedType();
    public static final AnnotatedType listOfWildcardNumbers = new TypeToken<GenericItemRepo<@NonNull List<? extends Number>>>() {
    }.getAnnotatedType();

    @Test
    public void testNonNullGenerics() {
        GenericItemRepo<@NonNull String> nonNullStringService = new GenericItemRepo<>();
        nonNullStringService.addItem("pooch", "Strudel, the poodle");
        nonNullStringService.addItem("booze", "Fire-water");

        GraphQLSchema schemaWithNonNullGenerics = new GraphQLSchemaBuilder()
                .withSingletonQuerySource(nonNullStringService, nonNullString)
                .build();

        GraphQLOutputType itemType = schemaWithNonNullGenerics.getQueryType().getFieldDefinition("getItem").getType();
        assertNonNull(itemType, Scalars.GraphQLString);

        GraphQLOutputType itemCollectionType = schemaWithNonNullGenerics.getQueryType().getFieldDefinition("getAllItems").getType();
        assertListOfNonNull(itemCollectionType, Scalars.GraphQLString);

        GraphQLFieldDefinition addOneItem = schemaWithNonNullGenerics.getMutationType().getFieldDefinition("addItem");
        assertEquals(addOneItem.getArguments().size(), 2);
        assertArgumentsPresent(addOneItem, "item", "name");
        assertNonNull(addOneItem.getArgument("item").getType(), Scalars.GraphQLString);

        GraphQLFieldDefinition addManyItems = schemaWithNonNullGenerics.getMutationType().getFieldDefinition("addItems");
        assertEquals(addManyItems.getArguments().size(), 1);
        assertArgumentsPresent(addManyItems, "items");
        assertListOfNonNull(addManyItems.getArgument("items").getType(), Scalars.GraphQLString);

        GraphQL graphQL = new GraphQL(schemaWithNonNullGenerics);
        ExecutionResult result = graphQL.execute("{ getAllItems }");
        assertTrue(result.getErrors().isEmpty());
        assertEquals(new ArrayList<>(nonNullStringService.getAllItems()), ((Map<String, Object>) result.getData()).get("getAllItems"));
    }

    @Test
    public void testRelayIdGenerics() {
        GenericItemRepo<@RelayId Date> dateIdService = new GenericItemRepo<>();
        dateIdService.addItem("firstEvent", new Date(1000));
        dateIdService.addItem("secondEvent", new Date(2000));

        GraphQLSchema schemaWithDateIds = new GraphQLSchemaBuilder()
                .withSingletonQuerySource(dateIdService, dateId)
                .build();

        GraphQLOutputType itemType = schemaWithDateIds.getQueryType().getFieldDefinition("getItem").getType();
        assertEquals(itemType, Scalars.GraphQLID);

        GraphQLOutputType itemCollectionType = schemaWithDateIds.getQueryType().getFieldDefinition("getAllItems").getType();
        assertListOf(itemCollectionType, Scalars.GraphQLID);

        GraphQLFieldDefinition addOneItem = schemaWithDateIds.getMutationType().getFieldDefinition("addItem");
        assertEquals(addOneItem.getArgument("item").getType(), Scalars.GraphQLID);

        GraphQLFieldDefinition addManyItems = schemaWithDateIds.getMutationType().getFieldDefinition("addItems");
        assertListOf(addManyItems.getArgument("items").getType(), Scalars.GraphQLID);

        GraphQL graphQL = new GraphQL(schemaWithDateIds);
        ExecutionResult result = graphQL.execute("{ contains(item: \"bnVsbDoxMDAw\") }");
        assertTrue(result.getErrors().isEmpty());
        assertEquals("bnVsbDoxMDAw", ((Map<String, Object>) result.getData()).get("contains"));
        //Search again but using raw (non Relay encoded) ID this time. For now, this is supported.
        result = graphQL.execute("{ contains(item: \"1000\") }");
        assertTrue(result.getErrors().isEmpty());
        assertEquals("bnVsbDoxMDAw", ((Map<String, Object>) result.getData()).get("contains"));
    }

    @Test
    public void testWildcardGenerics() {
        GenericItemRepo<@NonNull List<? extends Number>> wildcardNumberService = new GenericItemRepo<>();
        wildcardNumberService.addItem("player1", Arrays.asList(12, 13.4, new BigDecimal("4000")));
        wildcardNumberService.addItem("player2", Arrays.asList(new BigDecimal("12.56"), 14.78));

        GraphQLSchema schemaWithGenerics = new GraphQLSchemaBuilder()
                .withSingletonQuerySource(wildcardNumberService, listOfWildcardNumbers)
                .build();

        GraphQLOutputType itemType = schemaWithGenerics.getQueryType().getFieldDefinition("getItem").getType();
        assertNonNull(itemType, GraphQLList.class);
        assertListOf(((GraphQLNonNull) itemType).getWrappedType(), Scalars.GraphQLBigDecimal);

        GraphQLOutputType itemCollectionType = schemaWithGenerics.getQueryType().getFieldDefinition("getAllItems").getType();
        assertListOfNonNull(itemCollectionType, GraphQLList.class);
        assertListOf(((GraphQLNonNull) ((GraphQLList) itemCollectionType).getWrappedType()).getWrappedType(), Scalars.GraphQLBigDecimal);

        GraphQLFieldDefinition addOneItem = schemaWithGenerics.getMutationType().getFieldDefinition("addItem");
        GraphQLType itemArgType = addOneItem.getArgument("item").getType();
        assertNonNull(itemArgType, GraphQLList.class);
        assertListOf(((GraphQLNonNull) itemArgType).getWrappedType(), Scalars.GraphQLBigDecimal);

        GraphQL graphQL = new GraphQL(schemaWithGenerics);
        ExecutionResult result = graphQL.execute("{ getAllItems }");
        assertTrue(result.getErrors().isEmpty());
        Object[] expected = wildcardNumberService.getAllItems().toArray();
        Object[] actual = wildcardNumberService.getAllItems().toArray();
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(((Collection<?>) expected[i]).toArray(), ((Collection<?>) actual[i]).toArray());
        }
    }

    private static void assertNonNull(GraphQLType wrapperType, GraphQLType wrappedType) {
        assertEquals(GraphQLNonNull.class, wrapperType.getClass());
        assertEquals(((GraphQLNonNull) wrapperType).getWrappedType(), wrappedType);
    }

    private static void assertNonNull(GraphQLType wrapperType, Class<?> wrappedTypeClass) {
        assertEquals(GraphQLNonNull.class, wrapperType.getClass());
        assertEquals(((GraphQLNonNull) wrapperType).getWrappedType().getClass(), wrappedTypeClass);
    }

    private static void assertListOfNonNull(GraphQLType wrapperType, GraphQLType wrappedType) {
        assertEquals(wrapperType.getClass(), GraphQLList.class);
        assertEquals(((GraphQLList) wrapperType).getWrappedType().getClass(), GraphQLNonNull.class);
        assertEquals(((GraphQLNonNull) (((GraphQLList) wrapperType).getWrappedType())).getWrappedType(), wrappedType);
    }

    private static void assertListOfNonNull(GraphQLType wrapperType, Class<? extends GraphQLModifiedType> wrappedTypeClass) {
        assertEquals(wrapperType.getClass(), GraphQLList.class);
        assertEquals(((GraphQLList) wrapperType).getWrappedType().getClass(), GraphQLNonNull.class);
        assertEquals(((GraphQLNonNull) (((GraphQLList) wrapperType).getWrappedType())).getWrappedType().getClass(), wrappedTypeClass);
    }

    private static void assertListOf(GraphQLType wrapperType, GraphQLType wrappedType) {
        assertEquals(GraphQLList.class, wrapperType.getClass());
        assertEquals(wrappedType, ((GraphQLList) wrapperType).getWrappedType());
    }

    private static void assertArgumentsPresent(GraphQLFieldDefinition field, String... argumentNames) {
        Set<String> argNames = field.getArguments().stream().map(GraphQLArgument::getName).collect(Collectors.toSet());
        Arrays.stream(argumentNames).forEach(argName -> assertTrue(argNames.contains(argName)));
    }
}