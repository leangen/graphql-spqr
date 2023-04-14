package io.leangen.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import io.leangen.graphql.services.UserService;
import io.leangen.graphql.util.GraphQLUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.concurrent.atomic.AtomicInteger;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class SchemaTest {

    private static final String simpleQuery = "{users(regDate: 1465667452785) {" +
            "id, name, title, zmajs, addresses {" +
            "types" +
            "}}}";

    private static final String simpleQueryWithNullInput = "{usersByDate {" +
            "id, name, title, zmajs, addresses {" +
            "types" +
            "}}}";

    private static final String complexInputQuery = "{users (education: {" +
            "schoolName: \"tshc\"," +
            "startYear: 1999," +
            "endYear: 2003}) {" +
            "name" +
            "}}";

    private static final String complexGenericInputQuery = "{usersArr (educations: [" +
            "{schoolName: \"tshc\"," +
            "startYear: 1999," +
            "endYear: 2003," +
            "tier: TOP}," +

            "{schoolName: \"other\"," +
            "startYear: 1999," +
            "endYear: 2003," +
            "tier: BOTTOM}]) {" +
            "name" +
            "}}";

    private static final String mutation = "mutation M {" +
            "  updateUsername(username: \"OMG NEW USERNAME!!11\") {" +
            "    id" +
            "    name" +
            "    addresses {" +
            "      types" +
            "    }" +
            "  }" +
            "}";

    private static final String mapInputMutation = "mutation M {" +
            "upMe (updates: {" +
            "       key: \"name\"," +
            "       value: \"New Dyno\"}) {" +
            "   key" +
            "   value" +
            "}}";

    private static final String simpleFragmentQuery = "{" +
            "  users(regDate: 1465667452785) {" +
            "    ...userInfo" +
            "    uuid" +
            "  }" +
            "}" +
            "" +
            "fragment userInfo on User_String {" +
            "  name," +
            "  title," +
            "  regDate" +
            "}";

    @Parameterized.Parameter
    public ValueMapperFactory<?> valueMapperFactory;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static ValueMapperFactory<?>[] data() {
        return new ValueMapperFactory[] { new JacksonValueMapperFactory(), new GsonValueMapperFactory() };
    }

    @Test
    public void testBeanSupplier() {
        AtomicInteger counter = new AtomicInteger();

        GraphQLSchema schema = new TestSchemaGenerator()
                .withValueMapperFactory(valueMapperFactory)
                .withOperationsFromBean(() -> new Dynamic(counter.incrementAndGet()), Dynamic.class)
                .generate();
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        assertValueAtPathEquals(1, graphQL.execute("{number}"), "number");
        assertValueAtPathEquals(2, graphQL.execute("{number}"), "number");
    }

    @Test
    public void testSchema() {
        ExecutableSchema schema = new TestSchemaGenerator()
                .withValueMapperFactory(valueMapperFactory)
                .withTypeAdapters(new MapToListTypeAdapter())
                .withOperationsFromSingleton(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType())
                .generateExecutable();

        schema.getSchema().getQueryType().getFieldDefinitions().forEach(fieldDef -> {
            if (!GraphQLUtils.isRelayNodeInterface(fieldDef.getType())) {
                //All operations must have the underlying Java element mapped
                assertNotNull(schema.getTypeRegistry().getMappedOperation(FieldCoordinates.coordinates(schema.getSchema().getQueryType(), fieldDef)));
            }
        });

        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();
        ExecutionResult result;

        result = execute(exe, simpleFragmentQuery);
        assertNoErrors(result);
        result = execute(exe, complexGenericInputQuery);
        assertNoErrors(result);
        result = execute(exe, simpleQuery);
        assertNoErrors(result);
        result = execute(exe, simpleQueryWithNullInput);
        assertNoErrors(result);
        result = execute(exe, mapInputMutation);
        assertNoErrors(result);
    }

    private ExecutionResult execute(GraphQL graphQL, String operation) {
        return graphQL.execute(ExecutionInput.newExecutionInput().query(operation).build());
    }

    public static class Dynamic {

        private final int number;

        Dynamic(int number) {
            this.number = number;
        }

        @GraphQLQuery
        public int number() {
            return this.number;
        }
    }
}
