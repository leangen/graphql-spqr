package io.leangen.graphql;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultEdge;
import graphql.relay.Edge;
import graphql.relay.PageInfo;
import graphql.relay.Relay;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.geantyref.TypeToken;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.domain.Cat;
import io.leangen.graphql.domain.Character;
import io.leangen.graphql.domain.Dog;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Human;
import io.leangen.graphql.domain.Pet;
import io.leangen.graphql.domain.Robot;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.domain.User;
import io.leangen.graphql.execution.relay.Connection;
import io.leangen.graphql.execution.relay.Page;
import io.leangen.graphql.execution.relay.generic.PageFactory;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.common.MapToListTypeAdapter;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.services.UserService;
import io.leangen.graphql.support.TestLog;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Urls;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static io.leangen.graphql.support.QueryResultAssertions.assertValueAtPathEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
    public void testRelayMutations() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new UserService<Education>(), new TypeToken<UserService<Education>>(){}.getAnnotatedType())
                .withTypeAdapters(new MapToListTypeAdapter<>())
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

    @Test
    public void testExtendedPageMapping() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new ExtendedPageBookService())
                .generate();

        GraphQLFieldDefinition totalCount = schema.getObjectType("BookConnection")
                .getFieldDefinition("totalCount");
        assertNotNull(totalCount);
        assertEquals(Scalars.GraphQLLong, totalCount.getType());
        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();

        ExecutionResult result = exe.execute("{extended(first:10, after:\"20\") {" +
                "   totalCount" +
                "   pageInfo {" +
                "       hasNextPage" +
                "   }," +
                "   edges {" +
                "       cursor, node {" +
                "           title" +
                "}}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals(100L, result, "extended.totalCount");
        assertValueAtPathEquals("Tesseract", result, "extended.edges.0.node.title");
    }

    @Test
    public void testExtendedConnectionMapping() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new ExtendedEdgeBookService())
                .generate();

        GraphQLObjectType bookConnection = schema.getObjectType("BookConnection");
        assertEquals(3, bookConnection.getFieldDefinitions().size());
        GraphQLFieldDefinition totalCount = bookConnection.getFieldDefinition("totalCount");
        assertNotNull(totalCount);
        assertEquals(Scalars.GraphQLLong, totalCount.getType());

        GraphQLObjectType bookEdge = schema.getObjectType("BookEdge");
        assertEquals(3, bookEdge.getFieldDefinitions().size());
        GraphQLFieldDefinition color = bookEdge.getFieldDefinition("color");
        assertNotNull(color);
        assertTrue(color.getType() instanceof GraphQLEnumType);

        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();

        ExecutionResult result = exe.execute("{extended(first:10, after:\"20\") {" +
                "   totalCount" +
                "   pageInfo {" +
                "       hasNextPage" +
                "   }," +
                "   edges {" +
                "       color, cursor, node {" +
                "           title" +
                "}}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals(100L, result, "extended.totalCount");
        assertValueAtPathEquals("Tesseract", result, "extended.edges.0.node.title");
    }

    @Test
    public void testConflictingConnections() {
        try (TestLog log = new TestLog(OperationMapper.class)) {
            new TestSchemaGenerator()
                    .withOperationsFromSingleton(new ConflictingBookService())
                    .generate();
            assertEquals(1, log.getEvents().size());
            ILoggingEvent event = log.getEvents().get(0);
            assertThat(event.getLevel(), is(Level.WARN));
            assertThat(event.getMessage(), containsString(Urls.Errors.NON_UNIQUE_TYPE_NAME));
        }
    }

    @Test(expected = TypeMappingException.class)
    public void testInvalidPaginationArguments() {
        new TestSchemaGenerator()
                .withOperationsFromSingleton(new InvalidPagingService())
                .generate();
    }

    @Test
    public void testDirectNodeQuery() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingletons(new BookService())
                .generate();

        assertNotNull(schema.getQueryType().getFieldDefinition("node"));

        String id = "x123";
        String globalId = new Relay().toGlobalId(Book.class.getSimpleName(),"\"" + id + "\"");
        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{node(id: \"" + globalId + "\") {id ... on Book {title}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals(globalId, result, "node.id");
        assertValueAtPathEquals("Node Book", result, "node.title");
    }

    @Test
    public void testDirectNodeQueryComplexId() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingletons(new BookService(), new DescriptorService())
                .generate();

        assertNotNull(schema.getQueryType().getFieldDefinition("node"));
        assertTrue(GraphQLUtils.isRelayId(((GraphQLObjectType)schema.getType("Descriptor")).getFieldDefinition("id")));
        assertTrue(GraphQLUtils.isRelayId((schema.getQueryType().getFieldDefinition("descriptor").getArgument("id"))));

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{node(id: \"Qm9vazp7InRpdGxlIjoiVGhlIGtleSBib29rIiwiaWQiOiI3NzcifQ==\") {id ... on Descriptor {text}}}");
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testPolymorphicNodeQuery() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders(new PublicResolverBuilder())
                .withOperationsFromSingletons(new PolymorphicPrimaryResolverService())
                .generate();

        assertNotNull(schema.getQueryType().getFieldDefinition("node"));

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{node(id: \"Q2F0OjY2Ng==\") {id}}");
        assertTrue(result.getErrors().isEmpty());
        result = exe.execute("{node(id: \"Um9ib3Q6NjY2\") {id ... on Robot {name}}}");
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testPartialUnionNodeQuery() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders(new PublicResolverBuilder())
                .withOperationsFromSingletons(new PartialUnionPrimaryResolverService())
                .generate();

        assertNotNull(schema.getQueryType().getFieldDefinition("node"));

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{node(id: \"Um9ib3Q6NjY2\") {id ... on Robot {name}}}");
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testDirectNodeQueryPriority() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders(new PublicResolverBuilder())
                .withOperationsFromSingletons(new DirectAndPolymorphicPrimaryResolverService())
                .generate();

        assertNotNull(schema.getQueryType().getFieldDefinition("node"));

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{node(id: \"Q2F0Ojg=\") {id ... on Cat {name}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Correct", result, "node.name");
        result = exe.execute("{node(id: \"RG9nOjk=\") {id ... on Dog {sound}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Correct", result, "node.sound");
    }

    @Test
    public void testExplicitNodeQuery() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withResolverBuilders(new PublicResolverBuilder())
                .withOperationsFromSingletons(new ExplicitNodeService())
                .generate();

        assertNotNull(schema.getQueryType().getFieldDefinition("node"));

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{node(id: \"Q2F0Ojg=\") {id ... on Cat {name}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Correct", result, "node.name");
    }

    @Test
    public void testRelayId() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new BookService())
                .generate();
        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();
        String id = "x123";
        String expectedId = new Relay().toGlobalId(Book.class.getSimpleName(),"\"" + id + "\"");
        ExecutionResult result = exe.execute("{books(first:10, after:\"\") {edges {node {title id}}}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Tesseract", result, "books.edges.0.node.title");
        assertValueAtPathEquals(expectedId, result, "books.edges.0.node.id");
        result = exe.execute("{book(id:\"" + expectedId + "\") {title id}}");
        assertTrue(result.getErrors().isEmpty());
        assertValueAtPathEquals("Node Book", result, "book.title");
        assertValueAtPathEquals(expectedId, result, "book.id");
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

        @JsonCreator
        Book(@JsonProperty("title") String title, @JsonProperty("id") String isbn) {
            this.title = title;
            this.isbn = isbn;
        }

        public String getTitle() {
            return title;
        }

        @GraphQLQuery(name = "id")
        public @GraphQLId(relayId = true) String getIsbn() {
            return isbn;
        }
    }

    public static class Descriptor {
        private Book book;
        private String text;

        @JsonCreator
        Descriptor(@JsonProperty("id") Book book, @JsonProperty("text") String text) {
            this.book = book;
            this.text = text;
        }

        @GraphQLQuery(name = "id")
        public @GraphQLId(relayId = true) Book getBook() {
            return book;
        }

        public String getText() {
            return text;
        }
    }

    public static class BookService {
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

        @GraphQLQuery
        public Book book(@GraphQLId(relayId = true) String isbn) {
            return new Book("Node Book", isbn);
        }
    }

    public static class DescriptorService {

        @GraphQLQuery
        public Descriptor descriptor(@GraphQLId(relayId = true) Book book) {
            return new Descriptor(book, "An imaginative book description");
        }

        @GraphQLQuery
        public Descriptor random() {
            return new Descriptor(null, UUID.randomUUID().toString());
        }
    }

    public static class PolymorphicPrimaryResolverService {

        public Pet pet(@GraphQLId(relayId = true) int id) {
            return id % 2 == 0 ? new Cat() : new Dog();
        }

        public Character character(@GraphQLId(relayId = true) int id) {
            return id % 2 == 0 ? new Robot("X3R0", "Zero") : new Human("Jack Alman", "Jackal");
        }
    }

    public static class PartialUnionPrimaryResolverService {

        public @GraphQLUnion(name = "Character") Human character(@GraphQLId(relayId = true) int id, String name) {
            return new Human("Jack Alman", "Jackal");
        }

        public @GraphQLUnion(name = "Character") Robot character(@GraphQLId(relayId = true) int id) {
            return new Robot("X3R0", "Zero");
        }
    }

    public static class DirectAndPolymorphicPrimaryResolverService {

        public Pet pet(@GraphQLId(relayId = true) int id) {
            return id % 2 == 0 ? new Cat("Wrong") : new Dog("Correct");
        }

        public Cat cat(@GraphQLId(relayId = true) int id) {
            return new Cat("Correct");
        }
    }

    public static class ExplicitNodeService {

        public Cat node(@GraphQLId(relayId = true) int id) {
            return new Cat("Correct");
        }

        public Cat cat(@GraphQLId(relayId = true) int id) {
            return new Cat("Wrong");
        }
    }

    private static class InvalidPagingService {

        @GraphQLQuery(name = "streets")
        public Page<Street> streets(@GraphQLArgument(name = "first") String first) {
            return null;
        }
    }

    public static class ExtendedPageBookService {
        @GraphQLQuery(name = "extended")
        public ExtendedPage<Book> getExtended(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            List<Book> books = new ArrayList<>();
            books.add(new Book("Tesseract", "x123"));
            long count = 100L;
            long offset = Long.parseLong(after);
            return PageFactory.createOffsetBasedPage(books, count, offset, (edges, info) -> new ExtendedPage<>(edges, info, count));
        }
    }

    public static class ExtendedEdgeBookService {
        @GraphQLQuery(name = "extended")
        public ExtendedConnection<ExtendedEdge<Book>> getExtended(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            List<Book> books = new ArrayList<>();
            books.add(new Book("Tesseract", "x123"));
            long count = 100L;
            long offset = Long.parseLong(after);
            Iterator<ExtendedEdge.COLOR> colors = Arrays.asList(ExtendedEdge.COLOR.WHITE, ExtendedEdge.COLOR.BLACK).iterator();
            return PageFactory.createOffsetBasedConnection(books, count, offset,
                    (node, cursor) -> new ExtendedEdge<>(node, cursor, colors.next()), (edges, info) -> new ExtendedConnection<>(edges, info, count));
        }
    }

    public static class ConflictingBookService {
        @GraphQLQuery(name = "empty")
        public Page<Book> getEmpty(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            return null;
        }

        @GraphQLQuery(name = "extended")
        public ExtendedPage<Book> getExtended(@GraphQLArgument(name = "first") int first, @GraphQLArgument(name = "after") String after) {
            return null;
        }
    }

    public static class ExtendedEdge<T> extends DefaultEdge<T> {

        private final COLOR color;

        ExtendedEdge(T node, ConnectionCursor cursor, COLOR color) {
            super(node, cursor);
            this.color = color;
        }

        public COLOR getColor() {
            return color;
        }

        public enum COLOR {
            BLACK, WHITE
        }
    }

    public static class ExtendedPage<N> implements Page<N> {

        private final List<Edge<N>> edges;
        private final PageInfo pageInfo;
        private final long totalCount;

        ExtendedPage(List<Edge<N>> edges, PageInfo pageInfo, long totalCount) {
            this.edges = edges;
            this.pageInfo = pageInfo;
            this.totalCount = totalCount;
        }

        @Override
        public List<Edge<N>> getEdges() {
            return edges;
        }

        @Override
        public PageInfo getPageInfo() {
            return pageInfo;
        }

        public long getTotalCount() {
            return totalCount;
        }
    }

    public static class ExtendedConnection<E extends Edge> implements Connection<E> {

        private final List<E> edges;
        private final PageInfo pageInfo;
        private final long totalCount;

        ExtendedConnection(List<E> edges, PageInfo pageInfo, long count) {
            this.edges = edges;
            this.pageInfo = pageInfo;
            this.totalCount = count;
        }

        @Override
        public List<E> getEdges() {
            return edges;
        }

        @Override
        public PageInfo getPageInfo() {
            return pageInfo;
        }

        public long getTotalCount() {
            return totalCount;
        }
    }
}
