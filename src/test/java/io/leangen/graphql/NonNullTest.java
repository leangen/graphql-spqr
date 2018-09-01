package io.leangen.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.generator.mapping.common.NonNullMapper;
import io.leangen.graphql.support.TestLog;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import static io.leangen.graphql.support.GraphQLTypeAssertions.assertNonNull;
import static io.leangen.graphql.support.LogAssertions.assertWarningsLogged;

public class NonNullTest {

    @Test
    public void testNonNullWithDefaultValueWarning() {
        try (TestLog log = new TestLog(NonNullMapper.class)) {
            new TestSchemaGenerator().withOperationsFromSingleton(new Service()).generate();
            assertWarningsLogged(log.getEvents(), "Non-null input field", "Non-null argument");
        }
    }

    @Test
    public void testJsr305NonNull() {
        GraphQLSchema schema = new TestSchemaGenerator().withOperationsFromSingleton(new Jsr305()).generate();
        GraphQLFieldDefinition field = schema.getQueryType().getFieldDefinition("nonNull");
        assertNonNull(field.getType(), Scalars.GraphQLString);
        assertNonNull(field.getArgument("in").getType(), Scalars.GraphQLString);
    }

    @Test
    public void testJsr380NonNull() {
        GraphQLSchema schema = new TestSchemaGenerator().withOperationsFromSingleton(new Jsr380()).generate();
        GraphQLFieldDefinition field = schema.getQueryType().getFieldDefinition("nonNull");
        assertNonNull(field.getType(), Scalars.GraphQLString);
        assertNonNull(field.getArgument("in").getType(), Scalars.GraphQLString);
    }

    public static class Service {
        @GraphQLQuery
        public Integer integerWithDefault(@GraphQLArgument(name = "in", defaultValue = "3") @GraphQLNonNull Integer in) {
            return in;
        }

        @GraphQLQuery
        public Item fieldWithDefault(Item in) {
            return in;
        }
    }

    public static class Jsr305 {
        @GraphQLQuery
        @Nonnull
        public String nonNull(@Nonnull String in) {
            return in;
        }
    }

    public static class Jsr380 {
        @GraphQLQuery
        @NotNull
        public String nonNull(@NotNull String in) {
            return in;
        }
    }

    public static class Item {
        @GraphQLInputField(name = "title", defaultValue = "<UNKNOWN>")
        public @GraphQLNonNull String name;
    }
}
