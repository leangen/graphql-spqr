package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.generator.mapping.strategy.JsonDefaultValueProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArgumentValueTest {

    @Test
    public void unescapedStringTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("Monkey");
        assertEquals("Monkey", defaultValue);
    }

    @Test
    public void escapedStringTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("\"Monkey\"");
        assertEquals("Monkey", defaultValue);
    }

    private Object constructDefaultValue(Object defaultValue) throws NoSuchMethodException {
        return new JsonDefaultValueProvider().getDefaultValue(
                BookService.class.getDeclaredMethod("findBook", String.class).getParameters()[0],
                GenericTypeReflector.annotate(String.class),
                defaultValue);
    }

    private static class BookService {

        @GraphQLQuery
        RelayTest.Book findBook(@GraphQLArgument(name = "title", defaultValue = "Monkey") String title) {
            return new RelayTest.Book("The Silent Monkey Club", "x123");
        }
    }
}
