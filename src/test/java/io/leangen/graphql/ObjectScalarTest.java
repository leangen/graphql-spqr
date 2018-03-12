package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.batched.BatchedExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.JsonPatch;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class ObjectScalarTest {

    public static class PatchService {

        @GraphQLQuery(name = "processPatches")
        public List<JsonPatch> processPatches(@GraphQLArgument(name = "args") List<JsonPatch> patches) {
            return patches;
        }
    }

    @Test
    public void testObjectScalar() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new PatchService())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).queryExecutionStrategy(new BatchedExecutionStrategy()).build();
        ExecutionResult result = exe.execute("{processPatches(args: {op: replace, path: \"/active\", value: 1}){op}}");
        assertTrue(result.getErrors().isEmpty());
    }
}
