package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.value.JsonDefaultValueProvider;
import org.eclipse.microprofile.graphql.Argument;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Query;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArgumentValueTest {

    private static final GlobalEnvironment ENVIRONMENT = new TestGlobalEnvironment();

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
        return new JsonDefaultValueProvider(ENVIRONMENT).getDefaultValue(
                BookService.class.getDeclaredMethod("findBook", type).getParameters()[0],
                GenericTypeReflector.annotate(type),
                defaultValue);
    }

    private static class BookService {

        @Query
        RelayTest.Book findBook(@Argument(value = "title") @DefaultValue("Monkey") String title) {
            return new RelayTest.Book("The Silent Monkey Club", "x123");
        }

        @Query
        RelayTest.Book findBook(@Argument(value = "author") @DefaultValue("Monkey") NestedQueryTest.Author author) {
            return new RelayTest.Book("The Silent Monkey Club", "x123");
        }
    }
}
