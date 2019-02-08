package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.execution.InvocationContext;
import io.leangen.graphql.execution.ResolverInterceptor;
import io.leangen.graphql.support.TestLog;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.Query;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static io.leangen.graphql.support.LogAssertions.assertWarningsLogged;
import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InterceptorTest {

    @Test
    public void authInterceptorTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new ProtectedService())
                .withResolverInterceptors(new AuthInterceptor())
                .generate();

        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .context(new User("RegularUser"))
                .query("{user}")
                .build();

        ExecutionResult result = graphQL.execute(input);
        assertNoErrors(result);

        input = input.transform(builder -> builder.query("{admin}"));
        try(TestLog log = new TestLog(SimpleDataFetcherExceptionHandler.class)) {
            result = graphQL.execute(input);
        }
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Access denied"));
    }

    @Test
    public void upperCaseInterceptorTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new TestService())
                .withResolverInterceptors(new InputStringUpperCaseInterceptor(), new InputIntDuplicatingInterceptor())
                .generate();

        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query("{test(string: \"wow\", int: 11)}")
                .build();

        ExecutionResult result = graphQL.execute(input);
        assertNoErrors(result);
        assertValueAtPathEquals("WOW22", result, "test");
    }

    @Test
    public void exceptionLogInterceptorTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new BrokenService())
                .withResolverInterceptors(new ExceptionLoggingInterceptor())
                .generate();

        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query("{test}")
                .build();

        try(TestLog log = new TestLog(ExceptionLoggingInterceptor.class)) {
            ExecutionResult result = graphQL.execute(input);
            assertNoErrors(result);
            assertValueAtPathEquals(null, result, "test");
            assertWarningsLogged(log.getEvents(), "Bad mojo");
        }
    }

    private static class AuthInterceptor implements ResolverInterceptor {

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            Auth auth = context.getResolver().getExecutable().getDelegate().getAnnotation(Auth.class);
            User currentUser = context.getResolutionEnvironment().dataFetchingEnvironment.getContext();
            if (auth != null && !currentUser.getRoles().containsAll(Arrays.asList(auth.rolesRequired()))) {
                throw new IllegalAccessException("Access denied"); // or return null
            }
            return continuation.proceed(context);
        }
    }

    private static class InputStringUpperCaseInterceptor implements ResolverInterceptor {

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            Object[] modifiedArgs = Arrays.stream(context.getArguments())
                    .map(arg -> arg instanceof String ? arg.toString().toUpperCase() : arg)
                    .toArray();
            context = context.transform(builder -> builder.withArguments(modifiedArgs));
            return continuation.proceed(context);
        }
    }

    private static class InputIntDuplicatingInterceptor implements ResolverInterceptor {

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            Object[] modifiedArgs = Arrays.stream(context.getArguments())
                    .map(arg -> arg instanceof Integer ? ((Integer) arg) * 2 : arg)
                    .toArray();
            context = context.transform(builder -> builder.withArguments(modifiedArgs));
            return continuation.proceed(context);
        }
    }

    private static class ExceptionLoggingInterceptor implements ResolverInterceptor {

        private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingInterceptor.class);

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) {
            try {
                return continuation.proceed(context);
            } catch (Exception e) {
                log.warn("Bad mojo!", e);
                return null;
            }
        }
    }

    private static class User {
        private final Set<String> roles;

        User(String... roles) {
            this.roles = new HashSet<>(Arrays.asList(roles));
        }

        Set<String> getRoles() {
            return roles;
        }
    }

    public static class ProtectedService {
        @Query
        @Auth(rolesRequired = {"Admin"})
        public String admin(@Argument(value = "in") String in) {
            return in;
        }

        @Query
        @Auth(rolesRequired = {"RegularUser"})
        public String user(@Argument(value = "in") String in) {
            return in;
        }
    }

    public static class TestService {
        @Query
        public String test(@Argument(value = "string") String s, @Argument(value = "int") int i) {
            return s + i;
        }
    }

    public static class BrokenService {

        @Query
        public String test() {
            throw new IllegalArgumentException("Always broken!");
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface Auth {
        String[] rolesRequired();
    }
}
