package io.leangen.graphql;

import org.junit.Test;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.generator.exceptions.TypeMappingException;

public class PaginationTest {

    @Test(expected = TypeMappingException.class)
    public void testInvalidPaginationArguments() throws NoSuchMethodException {
        new TestSchemaGenerator()
                .withOperationsFromSingleton(new InvalidPagingService())
                .generate();
    }

    private static class InvalidPagingService {

        @GraphQLQuery(name = "streets")
        public Page<Street> streets(@GraphQLArgument(name = "first") String first) {
            return null;
        }
    }
}
