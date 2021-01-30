package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.strategy.value.JsonDefaultValueProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArgumentValueTest {

    private static final GlobalEnvironment ENVIRONMENT = new TestGlobalEnvironment();

    @Test
    public void unescapedStringTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("Monkey", String.class).getValue();
        assertEquals("Monkey", defaultValue);
    }

    @Test
    public void escapedStringTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("\"Monkey\"", String.class).getValue();
        assertEquals("\"Monkey\"", defaultValue);
    }

    @Test
    public void objectTest() throws NoSuchMethodException {
        Object defaultValue = constructDefaultValue("{\"name\" : \"Loud Ape\"}", NestedQueryTest.Author.class).getValue();
        assertTrue(defaultValue instanceof NestedQueryTest.Author);
        assertEquals("Loud Ape", ((NestedQueryTest.Author) defaultValue).getName());
    }

    private DefaultValue constructDefaultValue(Object defaultValue, Class type) throws NoSuchMethodException {
        return new JsonDefaultValueProvider(ENVIRONMENT).getDefaultValue(
                BookService.class.getDeclaredMethod("findBook", type).getParameters()[0],
                GenericTypeReflector.annotate(type),
                new DefaultValue(defaultValue));
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
