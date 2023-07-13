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
import static org.junit.Assert.assertEquals;

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

    @Test
    public void testJsr380GroupNonNull() {
        GraphQLSchema schema = new TestSchemaGenerator().withOperationsFromSingleton(new Jsr380Group()).generate();
        GraphQLFieldDefinition field = schema.getQueryType().getFieldDefinition("nonNull");
        assertEquals(field.getType(), Scalars.GraphQLString);
        assertEquals(field.getArgument("in").getType(), Scalars.GraphQLString);
    }

    @Test
    public void testJsr380JakartaNonNull() {
        GraphQLSchema schema = new TestSchemaGenerator().withOperationsFromSingleton(new Jsr380Jakarta()).generate();
        GraphQLFieldDefinition field = schema.getQueryType().getFieldDefinition("nonNull");
        assertNonNull(field.getType(), Scalars.GraphQLString);
        assertNonNull(field.getArgument("in").getType(), Scalars.GraphQLString);
    }

    @Test
    public void testJsr380JakartaGroupNonNull() {
        GraphQLSchema schema = new TestSchemaGenerator().withOperationsFromSingleton(new Jsr380JakartaGroup()).generate();
        GraphQLFieldDefinition field = schema.getQueryType().getFieldDefinition("nonNull");
        assertEquals(field.getType(), Scalars.GraphQLString);
        assertEquals(field.getArgument("in").getType(), Scalars.GraphQLString);
    }

    private static class Service {
        @GraphQLQuery
        public Integer integerWithDefault(@GraphQLArgument(name = "in", defaultValue = "3") @GraphQLNonNull Integer in) {
            return in;
        }

        @GraphQLQuery
        public Item fieldWithDefault(Item in) {
            return in;
        }
    }

    private static class Jsr305 {
        @GraphQLQuery
        @Nonnull
        public String nonNull(@Nonnull String in) {
            return in;
        }
    }

    private static class Jsr380 {
        @GraphQLQuery
        @NotNull
        public String nonNull(@NotNull String in) {
            return in;
        }
    }

    private static class Jsr380Group {
        @GraphQLQuery
        @NotNull(groups = Jsr380Group.class)
        public String nonNull(@NotNull(groups = Jsr380Group.class) String in) {
            return in;
        }
    }

    private static class Jsr380Jakarta {
        @GraphQLQuery
        @jakarta.validation.constraints.NotNull
        public String nonNull(@jakarta.validation.constraints.NotNull String in) {
            return in;
        }
    }

    private static class Jsr380JakartaGroup {
        @GraphQLQuery
        @jakarta.validation.constraints.NotNull(groups = Jsr380JakartaGroup.class)
        public String nonNull(@jakarta.validation.constraints.NotNull(groups = Jsr380JakartaGroup.class) String in) {
            return in;
        }
    }

    private static class Item {
        @GraphQLInputField(name = "title", defaultValue = "<UNKNOWN>")
        public @GraphQLNonNull String name;
    }
}
