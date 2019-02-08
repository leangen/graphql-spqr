package io.leangen.graphql;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.EnumValue;
import io.leangen.graphql.annotations.Subscription;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Type;
import org.junit.Test;
import org.reactivestreams.Publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TypeInfoTest {

    @Test
    public void testExplicitTypeInfo() {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new BoringService())
                .generate();

        GraphQLObjectType boringService = (GraphQLObjectType) schema.getType("TheBlob");
        assertNotEquals(null, boringService);
        assertEquals("Blip Bloop", boringService.getDescription());

        for (GraphQLFieldDefinition field : new GraphQLFieldDefinition[] {
                schema.getQueryType().getFieldDefinition("staleQuery"),
                schema.getMutationType().getFieldDefinition("staleMutation"),
                schema.getSubscriptionType().getFieldDefinition("staleSubscription")}) {

            assertTrue(field.isDeprecated());
            assertEquals("Boring", field.getDeprecationReason());
        }

        assertFalse(schema.getQueryType().getFieldDefinition("freshQuery").isDeprecated());
        assertFalse(schema.getMutationType().getFieldDefinition("freshMutation").isDeprecated());
        assertFalse(schema.getSubscriptionType().getFieldDefinition("freshSubscription").isDeprecated());

        for (GraphQLFieldDefinition field : new GraphQLFieldDefinition[] {
                schema.getQueryType().getFieldDefinition("sneakyQuery"),
                schema.getMutationType().getFieldDefinition("sneakyMutation"),
                schema.getSubscriptionType().getFieldDefinition("sneakySubscription")}) {

            assertFalse(field.isDeprecated());
            assertEquals("Maybe hides its deprecation", field.getDescription());
        }
    }

    @Test
    public void testImplicitTypeInfo() {
        testImplicitTypeInfo(true);
        testImplicitTypeInfo(false);
    }

    private void testImplicitTypeInfo(boolean respectJavaDeprecation) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new BoringService(), new PublicResolverBuilder().withJavaDeprecationRespected(respectJavaDeprecation))
                .generate();

        for (GraphQLFieldDefinition field : new GraphQLFieldDefinition[] {
                schema.getQueryType().getFieldDefinition("staleQuery"),
                schema.getMutationType().getFieldDefinition("staleMutation"),
                schema.getSubscriptionType().getFieldDefinition("staleSubscription")}) {

            assertEquals(respectJavaDeprecation, field.isDeprecated());
            if (respectJavaDeprecation) {
                assertEquals("Deprecated", field.getDeprecationReason());
            }
        }

        assertFalse(schema.getQueryType().getFieldDefinition("freshQuery").isDeprecated());
        assertFalse(schema.getMutationType().getFieldDefinition("freshMutation").isDeprecated());
        assertFalse(schema.getSubscriptionType().getFieldDefinition("freshSubscription").isDeprecated());

        for (GraphQLFieldDefinition field : new GraphQLFieldDefinition[] {
                schema.getQueryType().getFieldDefinition("sneakyQuery"),
                schema.getMutationType().getFieldDefinition("sneakyMutation"),
                schema.getSubscriptionType().getFieldDefinition("sneakySubscription")}) {

            assertEquals(respectJavaDeprecation, field.isDeprecated());
            assertNull(field.getDescription());
        }
    }

    @Test
    public void testEnumValues() {
        testEnumValues(true);
        testEnumValues(false);
    }

    private void testEnumValues(boolean respectJavaDeprecation) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new BunnyService())
                .withJavaDeprecationRespected(respectJavaDeprecation)
                .generate();

        GraphQLEnumType bunnyType = (GraphQLEnumType) schema.getType("BunnyType");

        assertEquals("Types of bunnies", bunnyType.getDescription());

        assertNotEquals(null, bunnyType.getValue("IntenselyCute"));
        assertFalse(bunnyType.getValue("IntenselyCute").isDeprecated());

        assertFalse(bunnyType.getValue("ADORABLE").isDeprecated());
        assertEquals("Thoroughly adorable", bunnyType.getValue("ADORABLE").getDescription());

        assertTrue(bunnyType.getValue("MEH").isDeprecated());
        assertEquals("Impossible", bunnyType.getValue("MEH").getDeprecationReason());

        assertEquals(respectJavaDeprecation, bunnyType.getValue("AVERAGE").isDeprecated());
        if (respectJavaDeprecation) {
            assertEquals("Deprecated", bunnyType.getValue("AVERAGE").getDeprecationReason());
        }

        assertEquals(respectJavaDeprecation, bunnyType.getValue("Omg").isDeprecated());
    }

    @SuppressWarnings("unused")
    public static class BoringService {
        @Query
        @io.leangen.graphql.annotations.Deprecated("Boring")
        @Deprecated
        public Blob staleQuery() {
            return null;
        }

        @Query
        public String freshQuery() {
            return "";
        }

        @Query(value = "sneakyQuery", description = "Maybe hides its deprecation")
        @Deprecated
        public String trickyQuery() {
            return "";
        }

        @Mutation
        @io.leangen.graphql.annotations.Deprecated("Boring")
        @Deprecated
        public void staleMutation() {
        }

        @Mutation
        public void freshMutation() {
        }

        @Mutation(value = "sneakyMutation", description = "Maybe hides its deprecation")
        @Deprecated
        public void trickyMutation() {
        }

        @Subscription
        @io.leangen.graphql.annotations.Deprecated("Boring")
        @Deprecated
        public Publisher<String> staleSubscription() {
            return null;
        }

        @Subscription
        public Publisher<String> freshSubscription() {
            return null;
        }

        @Subscription(value = "sneakySubscription", description = "Maybe hides its deprecation")
        @Deprecated
        public Publisher<String> trickySubscription() {
            return null;
        }
    }

    @Type(value = "TheBlob", description = "Blip Bloop")
    static class Blob {}

    @SuppressWarnings("unused")
    public static class BunnyService {
        @Query
        public BunnyType bunnyType() {
            return BunnyType.ADORABLE;
        }
    }

    @SuppressWarnings("unused")
    @Type(description = "Types of bunnies")
    public enum BunnyType {
        @EnumValue(value = "IntenselyCute") CUTE,
        @EnumValue(description = "Thoroughly adorable") ADORABLE,
        @io.leangen.graphql.annotations.Deprecated("Impossible") MEH,
        @Deprecated AVERAGE,
        @Deprecated @EnumValue(value = "Omg") WOW
    }
}
