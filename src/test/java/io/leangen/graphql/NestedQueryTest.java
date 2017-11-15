package io.leangen.graphql;

import org.junit.Test;

import java.time.LocalDate;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLQuery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class NestedQueryTest {
    
    @Test
    public void testSameFieldNameOnMultipleTypes() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new BookService())
                .generate();

        GraphQLFieldDefinition topLevelAuthor = schema.getQueryType().getFieldDefinition("author");
        GraphQLFieldDefinition bookAuthor = ((GraphQLObjectType) schema.getType("Book")).getFieldDefinition("author");
        GraphQLFieldDefinition essayAuthor = ((GraphQLObjectType) schema.getType("Essay")).getFieldDefinition("author");
        assertNotEquals(null, topLevelAuthor);
        assertTrue(topLevelAuthor.getType() instanceof GraphQLObjectType);
        assertEquals(topLevelAuthor.getType(), bookAuthor.getType());
        assertEquals(topLevelAuthor.getType(), essayAuthor.getType());

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult res = exe.execute("{books(after: \"2012-12-12\") {title, author(age: 17) {name}}}");
        assertTrue(res.getErrors().isEmpty());

        res = exe.execute("{essay(title: \"The manifold and me\") {title, author {name}}}");
        assertTrue(res.getErrors().isEmpty());
    }
    
    public static class BookService {
        
        @GraphQLQuery(name = "books")
        public RelayTest.Book[] getBooks(@GraphQLArgument(name = "after") LocalDate after) {
            return new RelayTest.Book[] {new RelayTest.Book("Tesseract", "x123")};
        }

        @GraphQLQuery(name = "essay")
        public Essay getEssay(@GraphQLArgument(name = "title") String title) {
            return new Essay(title, 300);
        }

        @GraphQLQuery(name = "author")
        public Author getEssayAuthor(@GraphQLContext Essay essay) {
            return new Author("The writer of ' " + essay.getTitle() + "'");
        }

        @GraphQLQuery(name = "author")
        public Author getAuthor(@GraphQLContext RelayTest.Book book, @GraphQLArgument(name = "age") int age) {
            return new Author("Famous Author");
        }

        @GraphQLQuery(name = "author")
        public Author getAuthor(@GraphQLContext RelayTest.Book book, @GraphQLArgument(name = "award") String award) {
            return new Author("Award Winner");
        }

        @GraphQLQuery(name = "author")
        public Author findAuthor(@GraphQLArgument(name = "name") String name) {
            return new Author(name);
        }
    }

    public static class Essay {
        private String title;
        private int wordCount;

        public Essay(String title, int wordCount) {
            this.title = title;
            this.wordCount = wordCount;
        }

        public String getTitle() {
            return title;
        }

        public int getWordCount() {
            return wordCount;
        }
    }

    public static class Author {
        private String name;

        public Author(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
