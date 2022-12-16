package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.relay.Relay;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import io.leangen.graphql.services.GenericItemRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.leangen.graphql.support.GraphQLTypeAssertions.assertArgumentsPresent;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertInputMapOf;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertListOf;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertListOfNonNull;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertListOfRelayIds;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertMapOf;
import static io.leangen.graphql.support.GraphQLTypeAssertions.assertNonNull;
import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static io.leangen.graphql.util.GraphQLUtils.isRelayId;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
public class GenericsTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[] data() {
        return new Object[] { new JacksonValueMapperFactory(), new GsonValueMapperFactory() };
    }
    
    // IMPORTANT! All type declarations have to stay outside of tests (can not be inlined)
    // as the annotation parser treats them differently and discards the annotations otherwise.
    // This is a JDK8 bug: http://stackoverflow.com/questions/39952812
    private static final AnnotatedType nonNullString = new TypeToken<GenericItemRepo<@GraphQLNonNull String>>() {
    }.getAnnotatedType();
    private static final AnnotatedType dateId = new TypeToken<GenericItemRepo<@GraphQLId(relayId = true) Date>>() {
    }.getAnnotatedType();
    private static final AnnotatedType listOfWildcardNumbers = new TypeToken<GenericItemRepo<@GraphQLNonNull List<? extends Number>>>() {
    }.getAnnotatedType();
    private static final AnnotatedType arrayOfListsOfNumbers = new TypeToken<GenericItemRepo<@GraphQLNonNull List<Number> @GraphQLNonNull []>>() {
    }.getAnnotatedType();

    private static final GlobalEnvironment ENVIRONMENT = GlobalEnvironment.EMPTY;

    @Parameterized.Parameter
    public ValueMapperFactory valueMapperFactory;
    
    private static final String ERRORS = "Error(s) during query resolution";

    @Test
    public void testNonNullGenerics() {
        GenericItemRepo<@GraphQLNonNull String> nonNullStringService = new GenericItemRepo<>();
        nonNullStringService.addItem("pooch", "Strudel, the poodle");
        nonNullStringService.addItem("booze", "Fire-water");

        GraphQLSchema schemaWithNonNullGenerics = new TestSchemaGenerator()
                .withOperationsFromSingleton(nonNullStringService, nonNullString)
                .withValueMapperFactory(valueMapperFactory)
                .generate();

        GraphQLOutputType itemType = schemaWithNonNullGenerics.getQueryType().getFieldDefinition("item").getType();
        assertNonNull(itemType, Scalars.GraphQLString);

        GraphQLOutputType itemCollectionType = schemaWithNonNullGenerics.getQueryType().getFieldDefinition("allItems").getType();
        assertListOfNonNull(itemCollectionType, Scalars.GraphQLString);

        GraphQLFieldDefinition addOneItem = schemaWithNonNullGenerics.getMutationType().getFieldDefinition("addItem");
        assertEquals(addOneItem.getArguments().size(), 2);
        assertArgumentsPresent(addOneItem, "item", "name");
        assertNonNull(addOneItem.getArgument("item").getType(), Scalars.GraphQLString);

        GraphQLFieldDefinition addManyItems = schemaWithNonNullGenerics.getMutationType().getFieldDefinition("addItems");
        assertEquals(addManyItems.getArguments().size(), 1);
        assertArgumentsPresent(addManyItems, "items");
        assertListOfNonNull(addManyItems.getArgument("items").getType(), Scalars.GraphQLString);

        GraphQL graphQL = GraphQL.newGraphQL(schemaWithNonNullGenerics).build();
        ExecutionResult result = graphQL.execute("{ allItems }");
        assertTrue(ERRORS, result.getErrors().isEmpty());
        assertEquals(new ArrayList<>(nonNullStringService.getAllItems()), ((Map<String, Object>) result.getData()).get("allItems"));
    }

    @Test
    public void testRelayIdGenerics() {
        GenericItemRepo<@GraphQLId(relayId = true) Date> dateIdService = new GenericItemRepo<>();
        final Date firstEvent = new Date(1000);
        final Date secondEvent = new Date(2000);
        dateIdService.addItem("firstEvent", firstEvent);
        dateIdService.addItem("secondEvent", secondEvent);

        GraphQLSchema schemaWithDateIds = new TestSchemaGenerator()
                .withOperationsFromSingleton(dateIdService, dateId)
                .withValueMapperFactory(valueMapperFactory)
                .generate();

        GraphQLOutputType itemType = schemaWithDateIds.getQueryType().getFieldDefinition("item").getType();
        assertTrue(isRelayId(itemType));

        GraphQLOutputType itemCollectionType = schemaWithDateIds.getQueryType().getFieldDefinition("allItems").getType();
        assertListOfRelayIds(itemCollectionType);

        GraphQLFieldDefinition addOneItem = schemaWithDateIds.getMutationType().getFieldDefinition("addItem");
        assertTrue(isRelayId(addOneItem.getArgument("id")));

        GraphQLFieldDefinition addManyItems = schemaWithDateIds.getMutationType().getFieldDefinition("addItems");
        assertListOfRelayIds(addManyItems.getArgument("items").getType());
        
        GraphQL graphQL = GraphQL.newGraphQL(schemaWithDateIds).build();
        String jsonDate = valueMapperFactory.getValueMapper(Collections.emptyMap(), ENVIRONMENT).toString(firstEvent, GenericTypeReflector.annotate(Date.class));
        String relayId = new Relay().toGlobalId("Query", jsonDate);
        ExecutionResult result = graphQL.execute("{ contains(id: \"" + relayId+ "\") }");
        assertTrue(ERRORS, result.getErrors().isEmpty());
        assertEquals(relayId, ((Map<String, Object>) result.getData()).get("contains"));
        //Search again but using raw (non Relay encoded) ID this time. For now, this is supported.
        result = graphQL.execute("{ contains(id: \"" + jsonDate.replace("\"", "\\\"") + "\") }");
        assertTrue(ERRORS, result.getErrors().isEmpty());
        assertEquals(relayId, ((Map<String, Object>) result.getData()).get("contains"));
    }

    @Test
    public void testWildcardGenerics() {
        GenericItemRepo<@GraphQLNonNull List<? extends Number>> wildcardNumberService = new GenericItemRepo<>();
        wildcardNumberService.addItem("player1", Arrays.asList(12, 13.4, new BigDecimal("4000")));
        wildcardNumberService.addItem("player2", Arrays.asList(new BigDecimal("12.56"), 14.78));

        GraphQLSchema schemaWithGenerics = new TestSchemaGenerator()
                .withOperationsFromSingleton(wildcardNumberService, listOfWildcardNumbers)
                .withValueMapperFactory(valueMapperFactory)
                .generate();

        GraphQLOutputType itemType = schemaWithGenerics.getQueryType().getFieldDefinition("item").getType();
        assertNonNull(itemType, GraphQLList.class);
        assertListOf(((graphql.schema.GraphQLNonNull) itemType).getWrappedType(), ExtendedScalars.GraphQLBigDecimal);

        GraphQLOutputType itemCollectionType = schemaWithGenerics.getQueryType().getFieldDefinition("allItems").getType();
        assertListOfNonNull(itemCollectionType, GraphQLList.class);
        assertListOf(((graphql.schema.GraphQLNonNull) ((GraphQLList) itemCollectionType).getWrappedType()).getWrappedType(), ExtendedScalars.GraphQLBigDecimal);

        GraphQLFieldDefinition addOneItem = schemaWithGenerics.getMutationType().getFieldDefinition("addItem");
        GraphQLType itemArgType = addOneItem.getArgument("item").getType();
        assertNonNull(itemArgType, GraphQLList.class);
        assertListOf(((graphql.schema.GraphQLNonNull) itemArgType).getWrappedType(), ExtendedScalars.GraphQLBigDecimal);

        GraphQL graphQL = GraphQL.newGraphQL(schemaWithGenerics).build();
        ExecutionResult result = graphQL.execute("{ allItems }");
        assertTrue(ERRORS, result.getErrors().isEmpty());
        Object[] expected = wildcardNumberService.getAllItems().toArray();
        Object[] actual = wildcardNumberService.getAllItems().toArray();
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testArrayGenerics() {
        GenericItemRepo<@GraphQLNonNull List<Number> @GraphQLNonNull []> arrayNumberService = new GenericItemRepo<>();
        List<Number>[] array1 = (List<Number>[]) new List[1];
        array1[0] = Arrays.asList(12, 13.4, new BigDecimal("4000"));
        List<Number>[] array2 = (List<Number>[]) new List[1];
        array2[0] = Arrays.asList(new BigDecimal("12.56"), 14.78);
        arrayNumberService.addItem("scores1", array1);
        arrayNumberService.addItem("scores2", array2);

        GraphQLSchema schemaWithGenerics = new TestSchemaGenerator()
                .withOperationsFromSingleton(arrayNumberService, arrayOfListsOfNumbers)
                .withValueMapperFactory(valueMapperFactory)
                .generate();

        GraphQLOutputType itemType = schemaWithGenerics.getQueryType().getFieldDefinition("item").getType();
        assertNonNull(itemType, GraphQLList.class);
        GraphQLType inner = ((graphql.schema.GraphQLNonNull) itemType).getWrappedType();
        assertListOf(inner, graphql.schema.GraphQLNonNull.class);
        inner = ((graphql.schema.GraphQLNonNull) ((GraphQLList) inner).getWrappedType()).getWrappedType();
        assertListOf(inner, ExtendedScalars.GraphQLBigDecimal);

        GraphQLFieldDefinition addOneItem = schemaWithGenerics.getMutationType().getFieldDefinition("addItem");
        GraphQLType itemArgType = addOneItem.getArgument("item").getType();
        assertNonNull(itemArgType, GraphQLList.class);
        inner = ((graphql.schema.GraphQLNonNull) itemType).getWrappedType();
        assertListOf(inner, graphql.schema.GraphQLNonNull.class);
        inner = ((graphql.schema.GraphQLNonNull) ((GraphQLList) inner).getWrappedType()).getWrappedType();
        assertListOf(inner, ExtendedScalars.GraphQLBigDecimal);

        GraphQL graphQL = GraphQL.newGraphQL(schemaWithGenerics).build();
        ExecutionResult result = graphQL.execute("{ allItems }");
        assertTrue(ERRORS, result.getErrors().isEmpty());
        Object[] expected = arrayNumberService.getAllItems().toArray();
        Object[] actual = arrayNumberService.getAllItems().toArray();
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testVariableBoundGenerics() {
        Type type = TypeFactory.parameterizedClass(EchoService.class, Number.class);
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders(new PublicResolverBuilder("io.leangen"))
                .withOperationsFromSingleton(new EchoService(), type)
                .generate();

        GraphQLFieldDefinition query = schema.getQueryType().getFieldDefinition("echo");
        assertEquals(ExtendedScalars.GraphQLBigDecimal, query.getType());
        assertEquals(ExtendedScalars.GraphQLBigDecimal, query.getArgument("in").getType());

        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = graphQL.execute("{ echo (in: 3) }");
        assertTrue(ERRORS, result.getErrors().isEmpty());
        assertValueAtPathEquals(new BigDecimal(3), result, "echo");
    }

    @Test
    public void testMissingGenerics() {
        Type type = TypeFactory.parameterizedClass(EchoService.class, MissingGenerics.class);
        GraphQLSchema schema = new TestSchemaGenerator()
                .withValueMapperFactory(valueMapperFactory)
                .withOperationsFromSingleton(new EchoService(), type, new PublicResolverBuilder())
                .withTypeTransformer(new DefaultTypeTransformer(true, true))
                .withTypeAdapters(new MapToListTypeAdapter())
                .generate();

        GraphQLFieldDefinition query = schema.getQueryType().getFieldDefinition("echo");
        GraphQLObjectType output = (GraphQLObjectType) query.getType();
        assertMapOf(output.getFieldDefinition("raw").getType(), GraphQLScalarType.class, GraphQLScalarType.class);
        assertMapOf(output.getFieldDefinition("unbounded").getType(), GraphQLScalarType.class, GraphQLScalarType.class);

        GraphQLInputObjectType input = (GraphQLInputObjectType) query.getArgument("in").getType();
        assertInputMapOf(input.getFieldDefinition("raw").getType(), GraphQLScalarType.class, GraphQLScalarType.class);
        assertInputMapOf(input.getFieldDefinition("unbounded").getType(), GraphQLScalarType.class, GraphQLScalarType.class);

        GraphQL runtime = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = runtime.execute("{" +
                "echo (in: {" +
                "   raw: [{key: 2, value: 3}]" +
                "   unbounded: [{key: 2, value: 3}]" +
                "}) {" +
                "   raw {key, value}" +
                "   unbounded {key, value}" +
                "}}");
        assertNoErrors(result);
        assertValueAtPathEquals(2, result, "echo.raw.0.key");
        assertValueAtPathEquals(3, result, "echo.raw.0.value");
        assertValueAtPathEquals(2, result, "echo.unbounded.0.key");
        assertValueAtPathEquals(3, result, "echo.unbounded.0.value");
    }

    public interface GenericEcho<T> {
        <S extends T> S echo(S input);
    }

    public static class EchoService<G> implements GenericEcho<G> {
        @Override
        public <S extends G> S echo(@GraphQLArgument(name = "in") S input) {
            return input;
        }
    }

    public static class MissingGenerics {
        private final Map raw;
        private final Map<?, ?> unbounded;

        @JsonCreator
        public MissingGenerics(Map raw, Map<?, ?> unbounded) {
            this.raw = raw;
            this.unbounded = unbounded;
        }

        public Map<Integer, Integer> getRaw() {
            return raw;
        }

        public Map<?, ?> getUnbounded() {
            return unbounded;
        }
    }
}
