package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.domain.Address;
import io.leangen.graphql.domain.SimpleUser;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import org.junit.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UniquenessTest {

    private static final String thisPackage = UniquenessTest.class.getPackage().getName();

    @Test
    public void testRelayIdUniqueness() {
        GraphQLSchema schema = schemaFor(new RelayIdService());
        testRootQueryTypeUniqueness(schema);
        testRootQueryArgumentTypeUniqueness(schema);
    }

    @Test
    public void testScalarUniqueness() {
        GraphQLSchema schema = schemaFor(new ScalarService());
        testRootQueryTypeUniqueness(schema);
        testRootQueryArgumentTypeUniqueness(schema);
    }

    @Test
    public void testObjectScalarUniqueness() {
        GraphQLSchema schema = schemaFor(new ObjectScalarService());
        testRootQueryTypeUniqueness(schema);
        testRootQueryArgumentTypeUniqueness(schema);
    }

    @Test
    public void testPageUniqueness() {
        testRootQueryTypeUniqueness(schemaFor(new PagingService()));
    }

    @Test
    public void testEnumUniqueness() {
        GraphQLSchema schema = schemaFor(new EnumService());
        testRootQueryTypeUniqueness(schema);
        testRootQueryArgumentTypeUniqueness(schema);
    }

    @Test
    public void testMapUniqueness() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withTypeAdapters(new MapToListTypeAdapter<>())
                .withOperationsFromSingleton(new MapService(), new PublicResolverBuilder(thisPackage))
                .generate();
        testRootQueryTypeUniqueness(schema, type -> ((GraphQLList) type).getWrappedType());
        testRootQueryArgumentTypeUniqueness(schema, type -> ((GraphQLList) type).getWrappedType());
    }

    @Test
    public void testEnumMapUniqueness() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new EnumMapService(), new PublicResolverBuilder(thisPackage))
                .withValueMapperFactory(new GsonValueMapperFactory())
                .generate();
        testRootQueryTypeUniqueness(schema);
        testRootQueryArgumentTypeUniqueness(schema);
        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult res = exe.execute("{map(in: {Lucidus: 12, Tenebris: 13}) {Tenebris, Lucidus}}");
        assertEquals(0, res.getErrors().size());
        res = exe.execute("{mapAgain(in: {Lucidus: 12, Tenebris: 13}) {Tenebris, Lucidus}}");
        assertEquals(0, res.getErrors().size());
    }

    private GraphQLSchema schemaFor(Object service) {
        return new TestSchemaGenerator()
                .withOperationsFromSingleton(service, new PublicResolverBuilder(thisPackage))
                .generate();
    }

    private void testRootQueryTypeUniqueness(GraphQLSchema schema) {
        testRootQueryTypeUniqueness(schema, Function.identity());
    }

    private void testRootQueryTypeUniqueness(GraphQLSchema schema, Function<GraphQLType, GraphQLType> unwrapper) {
        List<GraphQLType> fieldTypes = schema.getQueryType().getFieldDefinitions().stream()
                .map(GraphQLFieldDefinition::getType)
                .map(unwrapper)
                .collect(Collectors.toList());
        assertEquals(2, fieldTypes.size());
        assertTrue(fieldTypes.stream().allMatch(type -> fieldTypes.get(0) == type));
    }

    private void testRootQueryArgumentTypeUniqueness(GraphQLSchema schema) {
        testRootQueryArgumentTypeUniqueness(schema, Function.identity());
    }

    private void testRootQueryArgumentTypeUniqueness(GraphQLSchema schema, Function<GraphQLType, GraphQLType> unwrapper) {
        List<GraphQLType> inputTypes = schema.getQueryType().getFieldDefinitions().stream()
                .flatMap(def -> def.getArguments().stream())
                .map(GraphQLArgument::getType)
                .map(unwrapper)
                .collect(Collectors.toList());
        assertEquals(2, inputTypes.size());
        assertTrue(inputTypes.stream().allMatch(type -> inputTypes.get(0) == type));
    }

    public static class RelayIdService {

        public @GraphQLId(relayId = true) String getId(@GraphQLId(relayId = true) String in) {
            return in;
        }

        public @GraphQLId(relayId = true) String getIdAgain(@GraphQLId(relayId = true) String in) {
            return in;
        }
    }

    public static class ScalarService {

        public UUID getUuid(UUID in) {
            return in;
        }

        public UUID getUuidAgain(UUID in) {
            return in;
        }
    }

    public static class ObjectScalarService {

        public @GraphQLScalar Address getAddress(@GraphQLScalar Address in) {
            return in;
        }

        public @GraphQLScalar Address getAddressAgain(@GraphQLScalar Address in) {
            return in;
        }
    }

    public static class EnumService {

        enum BLACK_OR_WHITE {
            BLACK, WHITE
        }

        public BLACK_OR_WHITE getEnum(BLACK_OR_WHITE bow) {
            return BLACK_OR_WHITE.BLACK;
        }

        public BLACK_OR_WHITE getEnumAgain(BLACK_OR_WHITE bow) {
            return BLACK_OR_WHITE.WHITE;
        }
    }

    public static class EnumMapService {

        enum LIGHT_OR_DARK {
            @GraphQLEnumValue(name = "Lucidus", description = "Bright") LIGHT,
            @GraphQLEnumValue(name = "Tenebris", description = "Shady") DARK
        }

        public EnumMap<LIGHT_OR_DARK, Number> map(@io.leangen.graphql.annotations.GraphQLArgument(name = "in") EnumMap<LIGHT_OR_DARK, Number> in) {
            return in;
        }

        public EnumMap<LIGHT_OR_DARK, Number> mapAgain(@io.leangen.graphql.annotations.GraphQLArgument(name = "in") EnumMap<LIGHT_OR_DARK, Number> in) {
            return in;
        }
    }

    public static class PagingService {

        public Page<SimpleUser> getUsers() {
            return null;
        }

        public Page<SimpleUser> getUsersAgain() {
            return null;
        }
    }

    public static class MapService {

        public Map<String, SimpleUser> getUsers(Map<String, SimpleUser> in) {
            return null;
        }

        public Map<String, SimpleUser> getUsersAgain(Map<String, SimpleUser> in) {
            return null;
        }
    }
}
