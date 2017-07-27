package io.leangen.graphql;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.leangen.graphql.domain.SimpleUser;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UniquenessTest {

    private static final String thisPackage = UniquenessTest.class.getPackage().getName();

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
        GraphQLSchema schema = schemaFor(new MapService());
        testRootQueryTypeUniqueness(schema, type -> ((GraphQLList) type).getWrappedType());
        testRootQueryArgumentTypeUniqueness(schema, type -> ((GraphQLList) type).getWrappedType());
    }

    private GraphQLSchema schemaFor(Object service) {
        return new PreconfiguredSchemaGenerator()
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
