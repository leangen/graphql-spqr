package io.leangen.graphql;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.User;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;
import io.leangen.graphql.services.UserService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests concerning Relay support
 */
public class RelayTest {

    private static final String nodeQuery = "{node(id: \"dXNlcjox\") {... on user {" +
            "      name" +
            "    }" +
            "... on Node {" +
            "      id" +
            "    }" +
            "}}";

    private static final String connectionQuery = "{user(id: \"dXNlcjox\") {" +
            "id, name, addresses(after:\"azx\" first:6 type:\"office\") {" +
            "pageInfo {" +
            "   hasNextPage" +
            "}, " +
            "edges {" +
            "   cursor, node {" +
            "       types, owner {" +
            "           addresses(type:\"secret\") {" +
            "               types" +
            "}}}}}}}";
    
    private static final String relayMapInputMutation = "mutation M {" +
            "upMe (input: {" +
            "       clientMutationId: \"123\"," +
            "       updates: {" +
            "           key: \"name\"," +
            "           value: \"New Dyno\"}}) {" +
            "   clientMutationId," +
            "   result {" +
            "       key" +
            "       value" +
            "}}}";

    @Test
    public void testOffsetBasedPageCreation() {
        List<User<String>> users = new UserService<String>().getUsersById(1);
        Page<User<String>> userPage = PageFactory.createOffsetBasedPage(users, 5, 0);
        assertTrue(userPage.getPageInfo().isHasNextPage());
        assertFalse(userPage.getPageInfo().isHasPreviousPage());
    }

    @Test
    public void relayMutationTest() {
        GraphQLSchema schema = new PreconfiguredSchemaGenerator()
                .withOperationsFromSingleton(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType())
                .withRelayCompliantMutations()
                .generate();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();

        ExecutionResult result = exe.execute(relayMapInputMutation);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void simpleConcurrencyTest() throws InterruptedException, ExecutionException {
        GraphQLSchema schema = new PreconfiguredSchemaGenerator()
                .withOperationsFromSingleton(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType())
                .withRelayCompliantMutations()
                .generate();

        List<String> context = Arrays.asList("xxx", "zzz", "yyy");
        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();

        List<CompletableFuture<Long>> futures = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                long start = System.currentTimeMillis();
                ExecutionResult result = exe.execute(relayMapInputMutation, context);
                if (!result.getErrors().isEmpty()) {
                    fail("Error during concurrent execution");
                }
                return System.currentTimeMillis() - start;
            }));
        }
        CompletableFuture<List<Long>> result = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
        System.out.println(result.get().stream().mapToLong(t -> t).sum()/1000d);
    }
}
