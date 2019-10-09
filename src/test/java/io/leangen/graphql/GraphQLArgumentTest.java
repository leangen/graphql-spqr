package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLScalar;
import org.junit.Test;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;

/**
 * Tests whether various argument injectors are doing their job
 */
public class GraphQLArgumentTest {

    private static final String ECHO = "hello";
    private static final String DEFAULT_ECHO_QUERY = "{echo(text:\"" + ECHO + "\")}";
    private static final String OVERRIDE_ECHO_QUERY = "{echo(override:\"" + ECHO + "\")}";

    @Test
    public void testDefaultArgumentName() {
        ExecutionResult result = getApi(new DefaultArgumentNameService()).execute(DEFAULT_ECHO_QUERY);
        assertNoErrors(result);
        assertValueAtPathEquals(ECHO, result, "echo");
    }

    @Test
    public void testAnnotatedDefaultArgumentName() {
        ExecutionResult result = getApi(new AnnotatedDefaultArgumentNameService()).execute(DEFAULT_ECHO_QUERY);
        assertNoErrors(result);
        assertValueAtPathEquals(ECHO, result, "echo");
    }

    @Test
    public void testAnnotatedOverrideArgumentName() {
        ExecutionResult result = getApi(new AnnotatedOverrideArgumentNameService()).execute(OVERRIDE_ECHO_QUERY);
        assertNoErrors(result);
        assertValueAtPathEquals(ECHO, result, "echo");
    }

    private GraphQL getApi(Object service) {
        return GraphQL.newGraphQL(
                new TestSchemaGenerator()
                        .withOperationsFromSingleton(service)
                        .generate())
                .build();
    }

    public static class DefaultArgumentNameService {
        @GraphQLQuery
        public @GraphQLScalar String echo(String text) {
            return text;
        }
    }

    public static class AnnotatedDefaultArgumentNameService {
        @GraphQLQuery
        public @GraphQLScalar String echo(@GraphQLArgument(description = "dummy") String text) {
            return text;
        }
    }

    public static class AnnotatedOverrideArgumentNameService {
        @GraphQLQuery
        public @GraphQLScalar String echo(@GraphQLArgument(name = "override", description = "dummy") String text) {
            return text;
        }
    }

}
