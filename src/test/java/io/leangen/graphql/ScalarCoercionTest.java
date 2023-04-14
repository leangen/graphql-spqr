package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Human;
import org.junit.Test;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;

public class ScalarCoercionTest {

    @Test
    public void testIdStringToNumberCoercion() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new CharacterService())
                .generate();
        GraphQL runtime = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = runtime.execute("{ human(id: \"1\") { name } }");
        assertNoErrors(result);
    }

    @Test
    public void testIdNumberCoercion() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new CharacterService())
                .generate();
        GraphQL runtime = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = runtime.execute("{ human(id: 1) { name } }");
        assertNoErrors(result);
    }

    public static class CharacterService {

        @GraphQLQuery
        public Human human(@GraphQLId @GraphQLNonNull Integer id) {
            return new Human(id + "", "");
        }
    }
}
