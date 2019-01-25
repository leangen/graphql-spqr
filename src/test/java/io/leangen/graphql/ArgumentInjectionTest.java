package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.leangen.graphql.annotations.Argument;
import io.leangen.graphql.annotations.Context;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.annotations.Query;
import io.leangen.graphql.domain.Street;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;

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
        ExecutionResult result = execute(getApi(SIMPLE), ECHO_QUERY, context);
        assertValueAtPathEquals(TARGET_VALUE, result, ECHO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectRootContextInjection() {
        RootContext context = new RootContext(TARGET_VALUE);
        ExecutionResult result = execute(getApi(SIMPLE), ECHO_QUERY, context);
        assertValueAtPathEquals(TARGET_VALUE, result, ECHO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTrickyRootContextInjection() {
        RootContext context = new TrickyRootContext(TARGET_VALUE + " random garbage");
        ExecutionResult result = execute(getApi(SIMPLE), ECHO_QUERY, context);
        assertValueAtPathEquals(TARGET_VALUE, result, ECHO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNullArgument() {
        ExecutionResult result = getApi(new SimpleService2()).execute(ECHO_QUERY);
        assertNoErrors(result);
        assertValueAtPathEquals(null, result, ECHO);
    }

    private GraphQL getApi(Object service) {
        return GraphQL.newGraphQL(
                new TestSchemaGenerator()
                        .withOperationsFromSingleton(service)
                        .generate())
                .build();
    }

    private ExecutionResult execute(GraphQL graphQL, String query, Object context) {
        return graphQL.execute(ExecutionInput.newExecutionInput()
                .query(query)
                .context(context)
                .build());
    }

    public static class SimpleService {
        @Query(value = ECHO)
        public String echoRootContext(@Context("target") String target) {
            return target;
        }
    }

    public static class SimpleService2 {
        @Query(value = ECHO)
        public @GraphQLScalar Street echoArgument(@Argument(value = "user") @GraphQLScalar Street street) {
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
