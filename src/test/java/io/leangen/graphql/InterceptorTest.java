package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.InvocationContext;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.execution.ResolverInterceptor;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.support.TestLog;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static io.leangen.graphql.support.LogAssertions.assertWarningsLogged;
import static io.leangen.graphql.support.QueryResultAssertions.*;
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
        try(TestLog log = TestLog.unsafe(SimpleDataFetcherExceptionHandler.class)) {
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
        ExceptionLoggingInterceptor interceptor = new ExceptionLoggingInterceptor();
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new BrokenService())
                .withResolverInterceptors(interceptor)
                .generate();

        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query("{test}")
                .build();

        try (TestLog log = new TestLog(ExceptionLoggingInterceptor.class)) {
            ExecutionResult result = graphQL.execute(input);
            assertNoErrors(result);
            assertValueAtPathEquals(null, result, "test");
            assertWarningsLogged(log.getEvents(), "Bad mojo");
            assertTrue(interceptor.exception instanceof IllegalArgumentException); //the exception must not be wrapped
        }
    }

    @Test
    public void interceptorOrderTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new TracedService())
                .withOutputConverters(new OutputConverter<String, String>() {
                    @Override
                    public String convertOutput(String original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
                        resolutionEnvironment.addError("Conversion: exit");
                        return original;
                    }

                    @Override
                    public boolean supports(AnnotatedElement element, AnnotatedType type) {
                        return type.getType() == String.class;
                    }
                })
                .withResolverInterceptors(new ErrorAppendingInterceptor("Inner 1"), new ErrorAppendingInterceptor("Inner 2"))
                .withOuterResolverInterceptors(new ErrorAppendingInterceptor("Outer 1"), new ErrorAppendingInterceptor("Outer 2"))
                .generate();

        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query("{trace}")
                .build();

       ExecutionResult result = graphQL.execute(input);
       assertErrorsEqual(result, "Outer 1: enter", "Outer 2: enter", "Inner 1: enter", "Inner 2: enter",
               "Resolver called", "Inner 2: exit", "Inner 1: exit", "Conversion: exit", "Outer 2: exit", "Outer 1: exit");
    }

    @Test
    public void interceptorOnNullTest() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new TracedService())
                .generate();

        GraphQL api = GraphQL.newGraphQL(schema).build();

        ExecutionResult result = api.execute("{nil}");
        assertErrorsEqual(result, "Resolver called");
    }

    private static class AuthInterceptor implements ResolverInterceptor {

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            Auth auth = context.getResolver().getExecutable().getDelegate().getAnnotation(Auth.class);
            User currentUser = context.getResolutionEnvironment().getDataFetchingEnvironment().getContext();
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

        Exception exception;
        private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingInterceptor.class);

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) {
            try {
                return continuation.proceed(context);
            } catch (Exception e) {
                log.warn("Bad mojo!", e);
                this.exception = e;
                return null;
            }
        }
    }

    private static class ErrorAppendingInterceptor implements ResolverInterceptor {

        private final String id;

        ErrorAppendingInterceptor(String id) {
            this.id = id;
        }

        @Override
        public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
            ResolutionEnvironment env = context.getResolutionEnvironment();
            env.addError(id + ": enter");
            Object result = continuation.proceed(context);
            env.addError(id + ": exit");
            return result;
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
        @GraphQLQuery
        @Auth(rolesRequired = {"Admin"})
        public String admin(@GraphQLArgument(name = "in") String in) {
            return in;
        }

        @GraphQLQuery
        @Auth(rolesRequired = {"RegularUser"})
        public String user(@GraphQLArgument(name = "in") String in) {
            return in;
        }
    }

    public static class TestService {
        @GraphQLQuery
        public String test(@GraphQLArgument(name = "string") String s, @GraphQLArgument(name = "int") int i) {
            return s + i;
        }
    }

    public static class TracedService {
        @GraphQLQuery
        public String trace(@GraphQLEnvironment ResolutionEnvironment env) {
            env.addError("Resolver called");
            return "";
        }

        @GraphQLQuery
        public String nil(@GraphQLEnvironment ResolutionEnvironment env) {
            env.addError("Resolver called");
            return null;
        }
    }

    public static class BrokenService {

        @GraphQLQuery
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
