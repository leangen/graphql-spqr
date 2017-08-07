package io.leangen.graphql;

import org.junit.Test;

import java.util.List;

import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.generator.exceptions.TypeMappingException;

import static org.junit.Assert.assertEquals;

public class PaginationTest {

    @Test
    public void testPaginationArguments() throws NoSuchMethodException {
        GraphQLSchema schema = new PreconfiguredSchemaGenerator()
                .withOperationsFromSingleton(new PagingService())
                .generate();
        List<graphql.schema.GraphQLArgument> arguments = schema.getQueryType().getFieldDefinition("streets").getArguments();
        assertEquals(4, arguments.size()); //no duplicates
        assertEquals(15, arguments.get(0).getDefaultValue()); //overridden "first" respected
    }

    @Test(expected = TypeMappingException.class)
    public void testInvalidPaginationArguments() throws NoSuchMethodException {
        new PreconfiguredSchemaGenerator()
                .withOperationsFromSingleton(new InvalidPagingService())
                .generate();
    }

    private static class PagingService {

        @GraphQLQuery(name = "streets")
        public Page<Street> streets(@GraphQLArgument(name = "first", defaultValue = "15") int first) {
            return null;
        }
    }

    private static class InvalidPagingService {

        @GraphQLQuery(name = "streets")
        public Page<Street> streets(@GraphQLArgument(name = "first") String first) {
            return null;
        }
    }
}
