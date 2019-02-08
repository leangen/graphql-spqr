package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.junit.Test;

import java.time.LocalDate;

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
        
        @Query(value = "books")
        public RelayTest.Book[] getBooks(@Argument(value = "after") LocalDate after) {
            return new RelayTest.Book[] {new RelayTest.Book("Tesseract", "x123")};
        }

        @Query(value = "essay")
        public Essay getEssay(@Argument(value = "title") String title) {
            return new Essay(title, 300);
        }

        @Query(value = "author")
        public Author getEssayAuthor(@Source Essay essay) {
            return new Author("The writer of ' " + essay.getTitle() + "'");
        }

        @Query(value = "author")
        public Author getAuthor(@Source RelayTest.Book book, @Argument(value = "age") int age) {
            return new Author("Famous Author");
        }

        @Query(value = "author")
        public Author getAuthor(@Source RelayTest.Book book, @Argument(value = "award") String award) {
            return new Author("Award Winner");
        }

        @Query(value = "author")
        public Author findAuthor(@Argument(value = "name") String name) {
            return new Author(name);
        }
    }

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
    public static class Author {
        private String name;

        @JsonCreator
        public Author(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
