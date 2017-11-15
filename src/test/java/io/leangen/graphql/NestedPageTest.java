package io.leangen.graphql;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;

import static org.junit.Assert.assertTrue;

public class NestedPageTest {
    
    @Test
    public void nestedPagesTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new VoteService())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult res = exe.execute("{" +
                "    users(first:50) {" +
                "        pageInfo {" +
                "            startCursor" +
                "            endCursor" +
                "            hasNextPage" +
                "        }" +
                "        edges {" +
                "            node {" +
                "                firstName" +
                "                lastName" +
                "                userVotes(first: 33, after: \"aaa\") {" +
                "                    pageInfo {" +
                "                        startCursor" +
                "                        endCursor" +
                "                        hasNextPage" +
                "                    }" +
                "                    edges {" +
                "                        node {" +
                "                            id" +
                "                        }" +
                "                    }" +
                "                }" +
                "            }" +
                "        }" +
                "    }" +
                "}");
        
        assertTrue(res.getErrors().isEmpty());
    }

    public static class VoteService {

        @GraphQLQuery(name = "users")
        public Page<User> getUsers(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            List<User> users = Arrays.asList(new User("Bojack", "Horseman"), new User("Diane", "Nguyen"));
            return PageFactory.createOffsetBasedPage(users, 10, 0);
        }

        @GraphQLQuery(name = "userVotes")
        public Page<Vote> getUserVotes(@GraphQLContext User user, @GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            List<Vote> votes = Arrays.asList(new Vote("x1", "http://bojack.example.com"), new Vote("x2", "http://diane.example.com"));
            return PageFactory.createOffsetBasedPage(votes, 10, 0);
        }
    }

    public static class Vote {
        private String id;
        private String url;

        public Vote(String id, String url) {
            this.id = id;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }
    }

    public static class User {

        private String firstName;
        private String lastName;

        public User(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }
}