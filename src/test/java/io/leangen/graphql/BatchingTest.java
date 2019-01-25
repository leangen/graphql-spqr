package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.batched.Batched;
import graphql.execution.batched.BatchedExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.Argument;
import io.leangen.graphql.annotations.Context;
import io.leangen.graphql.annotations.Query;
import io.leangen.graphql.annotations.Source;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.SimpleUser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatchingTest {

    @Test
    @SuppressWarnings("unchecked")
    public void batchingTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new CandidatesService())
                .generate();

        AtomicBoolean runBatched = new AtomicBoolean(false);
        @SuppressWarnings("deprecation")
        GraphQL batchExe = GraphQLRuntime.newGraphQL(schema).queryExecutionStrategy(new BatchedExecutionStrategy()).build();
        ExecutionResult result;
        result = batchExe.execute(ExecutionInput.newExecutionInput()
                .query("{candidates {educations {startYear}}}")
                .context(runBatched).build());
        assertTrue("Query didn't run in batched mode", runBatched.get());
        assertNoErrors(result);
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
    }

    public static class CandidatesService {
        @Query(value = "candidates")
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
        @Query
        public List<Education> educations(@Argument(value = "users") @Source List<SimpleUser> users, @Context AtomicBoolean flag) {
            assertEquals(3, users.size());
            flag.getAndSet(true);
            return users.stream()
                    .map(u -> u.getEducation(2000 + u.getFullName().charAt(0)))
                    .collect(Collectors.toList());
        }
    }
}
