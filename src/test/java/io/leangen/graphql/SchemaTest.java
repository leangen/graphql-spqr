package io.leangen.graphql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.User;
import io.leangen.graphql.domain.UserService;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SchemaTest {

    private static final String nodeQuery = "{node(id: \"dXNlcjox\") {... on user {" +
            "      name" +
            "    }" +
            "... on Node {" +
            "      id" +
            "    }" +
            "}}";

    private static final String simpleQuery = "{users(regDate: 1465667452785) {" +
            "id, name, title, zmajs, addresses {" +
            "types" +
            "}}}";
    
    private static final String simpleQueryWithNullInput = "{usersByDate {" +
            "id, name, title, zmajs, addresses {" +
            "types" +
            "}}}";

    private static final String connectionQuery = "{user(id: \"dXNlcjox\") {" +
            "id, name, addresses(after:\"azx\" first:6 type:\"office\") {" +
            "pageInfo {" +
            "hasNextPage" +
            "}, " +
            "edges {" +
            "cursor, node {" +
            "types, owner {" +
            "addresses(type:\"creep\") {" +
            "types" +
            "}}}}}}}";

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

    @Parameterized.Parameter
    public ValueMapperFactory valueMapperFactory;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[] data() {
        return new Object[] { new JacksonValueMapperFactory(), new GsonValueMapperFactory() };
    }

    @Test
    public void testSchema() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withValueMapperFactory(valueMapperFactory)
                .withOperationsFromSingleton(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType())
                .generate();

        List<String> context = Arrays.asList("xxx", "zzz", "yyy");
        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result;

        result = exe.execute(complexGenericInputQuery, context);
        assertTrue(result.getErrors().isEmpty());
        result = exe.execute(simpleQuery, context);
        assertTrue(result.getErrors().isEmpty());
        result = exe.execute(simpleQueryWithNullInput, context);
        assertTrue(result.getErrors().isEmpty());
        result = exe.execute(mapInputMutation, context);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testOffsetBasedPageCreation() {
        List<User<String>> users = new UserService<String>().getUsersById(1);
        Page<User<String>> userPage = PageFactory.createOffsetBasedPage(users, 5, 0);
        assertTrue(userPage.getPageInfo().isHasNextPage());
        assertFalse(userPage.getPageInfo().isHasPreviousPage());
    }
}
