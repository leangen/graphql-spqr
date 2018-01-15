package io.leangen.graphql;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.User;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.generator.mapping.strategy.ObjectScalarStrategy;
import io.leangen.graphql.services.UserService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    private static final String simplePagedQueryTemplate = "{#query(first:10, after:\"20\") {" +
            "   pageInfo {" +
            "       hasNextPage" +
            "   }," +
            "   edges {" +
            "       cursor, node {" +
            "           title" +
            "}}}}";
    
    @Test
    public void testOffsetBasedPageCreation() {
        List<User<String>> users = new UserService<String>().getUsersById(1);
        Page<User<String>> userPage = PageFactory.createOffsetBasedPage(users, 5, 0);
        assertTrue(userPage.getPageInfo().isHasNextPage());
        assertFalse(userPage.getPageInfo().isHasPreviousPage());
    }

    @Test
    public void relayMutationTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType())
                .withTypeAdapters(new MapToListTypeAdapter<>(new ObjectScalarStrategy()))
                .withDefaults()
                .withRelayCompliantMutations()
                .generate();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();

        ExecutionResult result = exe.execute(relayMapInputMutation);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testSimplePagedQuery() {
        testPagedQuery("books");
    }

    @Test
    public void testEmptyPagedQuery() {
        testPagedQuery("empty");
    }

    private void testPagedQuery(String query) {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new BookService())
                .generate();
        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();

        ExecutionResult result = exe.execute(simplePagedQueryTemplate.replace("#query", query));
        assertTrue(result.getErrors().isEmpty());
    }

    public static class Book {
        private String title;
        private String isbn;

        public Book(String title, String isbn) {
            this.title = title;
            this.isbn = isbn;
        }

        public String getTitle() {
            return title;
        }

        public String getIsbn() {
            return isbn;
        }
    }

    public class BookService {
        @GraphQLQuery(name = "books")
        public Page<Book> getBooks(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            List<Book> books = new ArrayList<>();
            books.add(new Book("Tesseract", "x123"));
            return PageFactory.createOffsetBasedPage(books, 100, 10);
        }

        @GraphQLQuery(name = "empty")
        public Page<Book> getEmpty(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            return PageFactory.createOffsetBasedPage(Collections.emptyList(), 100, 10);
        }
    }
}
