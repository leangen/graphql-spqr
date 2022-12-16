package io.leangen.graphql;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.strategy.value.JsonDefaultValueProvider;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.Defaults;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArgumentValueTest {

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
        assertTrue(defaultValue instanceof Map);
        assertEquals("Loud Ape", ((Map<?, ?>) defaultValue).get("name"));
        ValueMapper valueMapper = Defaults.valueMapperFactory().getValueMapper();
        NestedQueryTest.Author deserialized = valueMapper.fromInput(defaultValue, GenericTypeReflector.annotate(NestedQueryTest.Author.class));
        assertEquals("Loud Ape", deserialized.getName());
    }

    private DefaultValue constructDefaultValue(Object defaultValue, Class<?> type) throws NoSuchMethodException {
        return new JsonDefaultValueProvider().getDefaultValue(
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
