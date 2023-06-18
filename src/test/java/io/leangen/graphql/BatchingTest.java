package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.SimpleUser;
import io.leangen.graphql.execution.ResolutionEnvironment;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.leangen.graphql.support.QueryResultAssertions.assertErrorPathsEqual;
import static io.leangen.graphql.support.QueryResultAssertions.assertErrorsEqual;
import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BatchingTest {

    @Test
    public void batchErrorTest() {
        GraphQLSchemaGenerator generator = new TestSchemaGenerator()
                .withOperationsFromSingleton(new FriendService());
        ExecutableSchema schema = generator
                .generateExecutable();

        GraphQL run = GraphQLRuntime.newGraphQL(schema).build();
        ExecutionResult result = run.execute("{" +
                "people {" +
                "   name, " +
                "   friendsWithError {" +
                "       name" +
                "       friends (extra:11) {" +
                "           name" +
                "       }" +
                "   } " +
                "}}");

        assertEquals(2, result.getErrors().size());
        assertErrorPathsEqual(result, "people.0.friendsWithError", "people.1.friendsWithError");
        assertErrorsEqual(result, "💥", "💥");
        assertValueAtPathEquals("p1-friend-with-error-1", result, "people.0.friendsWithError.0.name");
        assertValueAtPathEquals("p1-friend-with-error-2", result, "people.0.friendsWithError.1.name");
        assertValueAtPathEquals("p2-friend-with-error-1", result, "people.1.friendsWithError.0.name");
        assertValueAtPathEquals("p2-friend-with-error-2", result, "people.1.friendsWithError.1.name");
    }

    public static class Person {
        public String name;

        public Person(String name) {
            this.name = name;
        }
    }

    @Test
    public void syncBatchingTest() {
        batchingTest("education");
    }

    @Test
    public void asyncBatchingTest() {
        batchingTest("educationAsync");
    }

    @Test
    public void syncParameterizedBatchingTest() {
        verifyTiers(batchingTest("none: education{startYear tier} top:education(tier: TOP){startYear tier} bottom:education(tier: BOTTOM)"));
    }

    @Test
    public void asyncParameterizedBatchingTest() {
        verifyTiers(batchingTest("none: educationAsync{startYear tier} top:educationAsync(tier: TOP){startYear tier} bottom:educationAsync(tier: BOTTOM)"));
    }

    @Test
    public void nestedParameterizedBatchTest() {
        GraphQLSchemaGenerator generator = new TestSchemaGenerator()
                .withOperationsFromSingleton(new FriendService());
        ExecutableSchema schema = generator
                .generateExecutable();

        GraphQL run = GraphQLRuntime.newGraphQL(schema).build();
        ExecutionResult result = run.execute("{" +
                "people {" +
                "   name, " +
                "   f1:friends(extra: 1) {" +
                "       name" +
                "       f11:friends (extra:11) {" +
                "           name" +
                "       }" +
                "   }, " +
                "   f2:friends(extra: 2) {" +
                "       name" +
                "       f21:friends (extra:21) {" +
                "           name" +
                "       }" +
                "   }" +
                "}}");
        assertNoErrors(result);
        assertValueAtPathEquals("p1-friend-1-param-1", result, "people.0.f1.0.name");
        assertValueAtPathEquals("p1-friend-2-param-1", result, "people.0.f1.1.name");
        assertValueAtPathEquals("p1-friend-1-param-2", result, "people.0.f2.0.name");
        assertValueAtPathEquals("p1-friend-2-param-2", result, "people.0.f2.1.name");

        assertValueAtPathEquals("p1-friend-1-param-1-friend-1-param-11", result, "people.0.f1.0.f11.0.name");
        assertValueAtPathEquals("p1-friend-1-param-1-friend-2-param-11", result, "people.0.f1.0.f11.1.name");
        assertValueAtPathEquals("p1-friend-1-param-2-friend-1-param-21", result, "people.0.f2.0.f21.0.name");
        assertValueAtPathEquals("p1-friend-1-param-2-friend-2-param-21", result, "people.0.f2.0.f21.1.name");

        assertValueAtPathEquals("p1-friend-2-param-1-friend-1-param-11", result, "people.0.f1.1.f11.0.name");
        assertValueAtPathEquals("p1-friend-2-param-1-friend-2-param-11", result, "people.0.f1.1.f11.1.name");
        assertValueAtPathEquals("p1-friend-2-param-2-friend-1-param-21", result, "people.0.f2.1.f21.0.name");
        assertValueAtPathEquals("p1-friend-2-param-2-friend-2-param-21", result, "people.0.f2.1.f21.1.name");

        assertValueAtPathEquals("p2-friend-1-param-1", result, "people.1.f1.0.name");
        assertValueAtPathEquals("p2-friend-2-param-1", result, "people.1.f1.1.name");
        assertValueAtPathEquals("p2-friend-1-param-2", result, "people.1.f2.0.name");
        assertValueAtPathEquals("p2-friend-2-param-2", result, "people.1.f2.1.name");

        assertValueAtPathEquals("p2-friend-1-param-1-friend-1-param-11", result, "people.1.f1.0.f11.0.name");
        assertValueAtPathEquals("p2-friend-1-param-1-friend-2-param-11", result, "people.1.f1.0.f11.1.name");
        assertValueAtPathEquals("p2-friend-1-param-2-friend-1-param-21", result, "people.1.f2.0.f21.0.name");
        assertValueAtPathEquals("p2-friend-1-param-2-friend-2-param-21", result, "people.1.f2.0.f21.1.name");

        assertValueAtPathEquals("p2-friend-2-param-1-friend-1-param-11", result, "people.1.f1.1.f11.0.name");
        assertValueAtPathEquals("p2-friend-2-param-1-friend-2-param-11", result, "people.1.f1.1.f11.1.name");
        assertValueAtPathEquals("p2-friend-2-param-2-friend-1-param-21", result, "people.1.f2.1.f21.0.name");
        assertValueAtPathEquals("p2-friend-2-param-2-friend-2-param-21", result, "people.1.f2.1.f21.1.name");
    }

    @SuppressWarnings("unchecked")
    private void verifyTiers(ExecutionResult result) {
        ((Map<String,List<Object>>) result.getData()).get("candidates").forEach( c  -> {
            assertNull(((Map<String, Object>) (((Map<String, Object>) c).get("none"))).get("tier"));
            assertEquals("TOP", ((Map<String, Object>) (((Map<String, Object>) c).get("top"))).get("tier"));
            assertEquals("BOTTOM", ((Map<String, Object>) (((Map<String, Object>) c).get("bottom"))).get("tier"));
        });
    }

    @SuppressWarnings("unchecked")
    private ExecutionResult batchingTest(String fieldName) {
        GraphQLSchemaGenerator generator = new TestSchemaGenerator()
                .withOperationsFromSingleton(new CandidatesService());
        ExecutableSchema schema = generator
                .generateExecutable();

        AtomicBoolean runBatched = new AtomicBoolean(false);
        GraphQL batchExe = GraphQLRuntime.newGraphQL(schema).build();
        ExecutionResult result;
        result = batchExe.execute(ExecutionInput.newExecutionInput()
                .query("{candidates {" + fieldName + " {startYear tier}}}")
                .context(runBatched)
                .build());
        assertNoErrors(result);
        assertTrue("Query didn't run in batched mode", runBatched.get());
        assertNoErrors(result);
        //noinspection rawtypes
        assertEquals(3, ((Map<String, List>) result.getData()).get("candidates").size());

        //TODO put this back when/if the ability to expose nested queries as top-level is reintroduced
        /*runBatched.getAndSet(false);
        GraphQL simpleExe = GraphQLRuntime.newGraphQL(schema).build();
        result = simpleExe.execute("{educations(users: [" +
                    "{fullName: \"One\"}," +
                    "{fullName: \"Two\"}," +
                    "{fullName: \"Three\"}" +
                "]) {startYear}}", runBatched);
        assertNoErrors(result);*/

        return result;
    }

    public static class CandidatesService {
        @GraphQLQuery(name = "candidates")
        public List<SimpleUser> getCandidates() {
            SimpleUser friend = new SimpleUser("Other Guy");
            SimpleUser one = new SimpleUser("One", friend);
            SimpleUser two = new SimpleUser("Two", friend);
            SimpleUser three = new SimpleUser("Three", friend);
            List<SimpleUser> dudes = new ArrayList<>();
            dudes.add(one);
            dudes.add(two);
            dudes.add(three);
            return dudes;
        }

        @Batched
        @GraphQLQuery
        public List<Education> education(@GraphQLContext List<SimpleUser> users,
                                         @GraphQLRootContext AtomicBoolean flag,
                                         @GraphQLArgument(name = "tier") Education.Tier tier) {
//            assertEquals(3, users.size());
            flag.getAndSet(true);
            return users.stream()
                    .map(u -> u.getEducation(1900 + u.getFullName().charAt(0)))
                    .peek(e -> e.tier = tier)
                    .collect(Collectors.toList());
        }

        @Batched
        @GraphQLQuery
        public CompletionStage<List<Education>> educationAsync(@GraphQLContext List<SimpleUser> users,
                                                               @GraphQLRootContext AtomicBoolean flag,
                                                               @GraphQLArgument(name = "tier") Education.Tier tier) {
            return CompletableFuture.supplyAsync(() -> education(users, flag, tier));
        }
    }

    public static class FriendService {

        @GraphQLQuery
        public List<Person> people() {
            return Arrays.asList(new Person("p1"), new Person("p2"));
        }

        @Batched
        @GraphQLQuery
        public List<List<Person>> friends(@GraphQLContext List<Person> people, int extra) {
            return people.stream()
                    .map(d -> Arrays.asList(
                            new Person(d.name + "-" + "friend-1-param-" + extra),
                            new Person(d.name + "-" + "friend-2-param-" + extra)))
                    .collect(Collectors.toList());
        }

        @Batched
        @GraphQLQuery
        public List<List<Person>> friendsWithError(@GraphQLContext List<Person> people, @GraphQLEnvironment ResolutionEnvironment env) {
            env.addError("💥");
            return people.stream()
                    .map(d -> Arrays.asList(
                            new Person(d.name + "-friend-with-error-1"),
                            new Person(d.name + "-friend-with-error-2")))
                    .collect(Collectors.toList());
        }
    }
}
