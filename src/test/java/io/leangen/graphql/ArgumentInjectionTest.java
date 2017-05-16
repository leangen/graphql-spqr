package io.leangen.graphql;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import graphql.ExecutionResult;
import graphql.GraphQL;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLRootContext;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.domain.Street;

import static io.leangen.graphql.assertions.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests whether various argument injectors are doing their job
 */
public class ArgumentInjectionTest {

    private static final SimpleService SIMPLE = new SimpleService();
    private static final String TARGET_VALUE = "a surprising string!";
    private static final String ECHO = "echo";
    private static final String ECHO_QUERY = "{echo}";

    @Test
    @SuppressWarnings("unchecked")
    public void testMapRootContextInjection() {
        Map<String, String> context = new HashMap<>();
        context.put("target", TARGET_VALUE);
        ExecutionResult result = getApi(SIMPLE).execute(ECHO_QUERY, context);
        assertValueAtPathEquals(TARGET_VALUE, result, ECHO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectRootContextInjection() {
        RootContext context = new RootContext(TARGET_VALUE);
        ExecutionResult result = getApi(SIMPLE).execute(ECHO_QUERY, context);
        assertValueAtPathEquals(TARGET_VALUE, result, ECHO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTrickyRootContextInjection() {
        RootContext context = new TrickyRootContext(TARGET_VALUE + " random garbage");
        ExecutionResult result = getApi(SIMPLE).execute(ECHO_QUERY, context);
        assertValueAtPathEquals(TARGET_VALUE, result, ECHO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNullArgument() {
        ExecutionResult result = getApi(new SimpleService2()).execute(ECHO_QUERY);
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals(null, result, ECHO);
    }

    private GraphQL getApi(Object service) {
        return GraphQL.newGraphQL(
                new PreconfiguredSchemaGenerator()
                        .withOperationsFromSingleton(service)
                        .generate())
                .build();
    }

    public static class SimpleService {
        @GraphQLQuery(name = ECHO)
        public String echoRootContext(@GraphQLRootContext("target") String target) {
            return target;
        }
    }

    public static class SimpleService2 {
        @GraphQLQuery(name = ECHO)
        public @GraphQLScalar Street echoArgument(@GraphQLArgument(name = "user") @GraphQLScalar Street street) {
            return street;
        }
    }

    public static class RootContext {
        private String target;

        RootContext(String target) {
            this.target = target;
        }

        public String getTarget() {
            return target;
        }
    }

    public static class TrickyRootContext extends RootContext {

        TrickyRootContext(String target) {
            super(target);
        }

        @Override
        public String getTarget() {
            return TARGET_VALUE; //getter doesn't return field value
        }
    }
}
