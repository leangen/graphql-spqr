package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.generator.mapping.strategy.JsonDefaultValueProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArgumentValueTest {

    @Test
    public void unescapedStringTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("Monkey", String.class);
        assertEquals("Monkey", defaultValue);
    }

    @Test
    public void escapedStringTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("\"Monkey\"", String.class);
        assertEquals("\"Monkey\"", defaultValue);
    }

    @Test
    public void objectTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("{\"name\" : \"Loud Ape\"}", NestedQueryTest.Author.class);
        assertTrue(defaultValue instanceof NestedQueryTest.Author);
        assertEquals("Loud Ape", ((NestedQueryTest.Author) defaultValue).getName());
    }

    private Object constructDefaultValue(Object defaultValue, Class type) throws NoSuchMethodException {
        return new JsonDefaultValueProvider().getDefaultValue(
                BookService.class.getDeclaredMethod("findBook", type).getParameters()[0],
                GenericTypeReflector.annotate(type),
                defaultValue);
    }

    private static class BookService {

        @GraphQLQuery
        RelayTest.Book findBook(@GraphQLArgument(name = "title", defaultValue = "Monkey") String title) {
            return new RelayTest.Book("The Silent Monkey Club", "x123");
        }

        @GraphQLQuery
        RelayTest.Book findBook(@GraphQLArgument(name = "author", defaultValue = "Monkey") NestedQueryTest.Author author) {
            return new RelayTest.Book("The Silent Monkey Club", "x123");
        }
    }
}
