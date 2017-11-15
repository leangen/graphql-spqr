package io.leangen.graphql;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.batched.Batched;
import graphql.execution.batched.BatchedExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLRootContext;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.SimpleUser;

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
        GraphQL batchExe = GraphQLRuntime.newGraphQL(schema).queryExecutionStrategy(new BatchedExecutionStrategy()).build();
        ExecutionResult result;
        result = batchExe.execute("{candidates {stuff {startYear}}}", runBatched);
        assertTrue("Query didn't run in batched mode", runBatched.get());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(3, ((Map<String, List>) result.getData()).get("candidates").size());

        //TODO put this back when/if the ability to expose nested queries as top-level is reintroduced
        /*runBatched.getAndSet(false);
        GraphQL simpleExe = GraphQLRuntime.newGraphQL(schema).build();
        result = simpleExe.execute("{stuff(users: [" +
                    "{fullName: \"One\"}," +
                    "{fullName: \"Two\"}," +
                    "{fullName: \"Three\"}" +
                "]) {startYear}}", runBatched);
        assertTrue(result.getErrors().isEmpty());*/
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
        public List<Education> stuff(@GraphQLArgument(name = "users") @GraphQLContext List<SimpleUser> users, @GraphQLRootContext AtomicBoolean flag) {
            assertEquals(3, users.size());
            flag.getAndSet(true);
            return users.stream()
                    .map(u -> u.getEducation(2000 + u.getFullName().charAt(0)))
                    .collect(Collectors.toList());
        }
    }
}
