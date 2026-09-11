package io.leangen.graphql;

import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.Defaults;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JacksonVersionCoexistenceTest {

    @Test
    public void bothJacksonValueMapperFactoriesAreAvailable() {
        assertNotNull(new io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory());
        assertNotNull(new io.leangen.graphql.metadata.strategy.value.jackson2.JacksonValueMapperFactory());
    }

    @Test
    public void defaultsCanSelectEitherJacksonVersionExplicitly() {
        ValueMapperFactory<?> jackson2 = Defaults.valueMapperFactory(Defaults.JsonLibrary.JACKSON_2);
        ValueMapperFactory<?> jackson3 = Defaults.valueMapperFactory(Defaults.JsonLibrary.JACKSON_3);

        assertEquals("JacksonValueMapperFactory", jackson2.getClass().getSimpleName());
        assertEquals("JacksonValueMapperFactory", jackson3.getClass().getSimpleName());
        assertEquals(2, Defaults.modules(Defaults.JsonLibrary.JACKSON_2).size());
        assertEquals(2, Defaults.modules(Defaults.JsonLibrary.JACKSON_3).size());
    }
}
