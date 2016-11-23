package io.leangen.graphql;

import org.junit.Test;

import java.lang.reflect.AnnotatedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.NonNull;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.domain.GenericItemRepo;

import static io.leangen.graphql.assertions.GraphQLTypeAssertions.assertArgumentsPresent;
import static io.leangen.graphql.assertions.GraphQLTypeAssertions.assertListOf;
import static io.leangen.graphql.assertions.GraphQLTypeAssertions.assertListOfNonNull;
import static io.leangen.graphql.assertions.GraphQLTypeAssertions.assertNonNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class GenericsTest {

    // IMPORTANT! All type declarations have to stay outside of tests (can not be inlined)
    // as the annotation parser treats them differently and discards the annotations otherwise.
    // This is JDK8 bug: http://stackoverflow.com/questions/39952812
    private static final AnnotatedType nonNullString = new TypeToken<GenericItemRepo<@NonNull String>>() {
    }.getAnnotatedType();
    private static final AnnotatedType dateId = new TypeToken<GenericItemRepo<@RelayId Date>>() {
    }.getAnnotatedType();
    private static final AnnotatedType listOfWildcardNumbers = new TypeToken<GenericItemRepo<@NonNull List<? extends Number>>>() {
    }.getAnnotatedType();
    private static final AnnotatedType arrayOfListsOfNumbers = new TypeToken<GenericItemRepo<@NonNull List<Number> @NonNull[]>>() {
    }.getAnnotatedType();

    @Test
    public void testNonNullGenerics() {
        GenericItemRepo<@NonNull String> nonNullStringService = new GenericItemRepo<>();
        nonNullStringService.addItem("pooch", "Strudel, the poodle");
        nonNullStringService.addItem("booze", "Fire-water");

        GraphQLSchema schemaWithNonNullGenerics = new GraphQLSchemaBuilder()
                .withSingletonQuerySource(nonNullStringService, nonNullString)
                .withDefaults()
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
                .withDefaults()
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
                .withDefaults()
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
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testArrayGenerics() {
        GenericItemRepo<@NonNull List<Number> @NonNull[]> arrayNumberService = new GenericItemRepo<>();
        List<Number>[] array1 = (List<Number>[]) new List[1];
        array1[0] = Arrays.asList(12, 13.4, new BigDecimal("4000"));
        List<Number>[] array2 = (List<Number>[]) new List[1];
        array2[0] = Arrays.asList(new BigDecimal("12.56"), 14.78);
        arrayNumberService.addItem("scores1", array1);
        arrayNumberService.addItem("scores2", array2);

        GraphQLSchema schemaWithGenerics = new GraphQLSchemaBuilder()
                .withSingletonQuerySource(arrayNumberService, arrayOfListsOfNumbers)
                .withDefaults()
                .build();

        GraphQLOutputType itemType = schemaWithGenerics.getQueryType().getFieldDefinition("getItem").getType();
        assertNonNull(itemType, GraphQLList.class);
        GraphQLType inner = ((GraphQLNonNull) itemType).getWrappedType();
        assertListOf(inner, GraphQLNonNull.class);
        inner = ((GraphQLNonNull) ((GraphQLList) inner).getWrappedType()).getWrappedType();
        assertListOf(inner, Scalars.GraphQLBigDecimal);

        GraphQLFieldDefinition addOneItem = schemaWithGenerics.getMutationType().getFieldDefinition("addItem");
        GraphQLType itemArgType = addOneItem.getArgument("item").getType();
        assertNonNull(itemArgType, GraphQLList.class);
        inner = ((GraphQLNonNull) itemType).getWrappedType();
        assertListOf(inner, GraphQLNonNull.class);
        inner = ((GraphQLNonNull) ((GraphQLList) inner).getWrappedType()).getWrappedType();
        assertListOf(inner, Scalars.GraphQLBigDecimal);

        GraphQL graphQL = new GraphQL(schemaWithGenerics);
        ExecutionResult result = graphQL.execute("{ getAllItems }");
        assertTrue(result.getErrors().isEmpty());
        Object[] expected = arrayNumberService.getAllItems().toArray();
        Object[] actual = arrayNumberService.getAllItems().toArray();
        assertArrayEquals(expected, actual);
    }
}
