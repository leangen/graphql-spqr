package io.leangen.graphql;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import org.junit.Test;
import org.reactivestreams.Publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OperationInfoTest {

    @Test
    public void testExplicitOperationInfo() {
        testOperationInfo(new AnnotatedResolverBuilder(), false);
    }

    @Test
    public void testImplicitOperationInfo() {
        testOperationInfo(new PublicResolverBuilder().withJavaDeprecationRespected(true), true);
        testOperationInfo(new PublicResolverBuilder().withJavaDeprecationRespected(false), false);
    }

    private void testOperationInfo(ResolverBuilder resolverBuilder, boolean sneakyFieldDeprecated) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingleton(new BoringService(), resolverBuilder)
                .generate();

        GraphQLObjectType boringService = (GraphQLObjectType) schema.getType("TheBlob");
        assertNotNull(boringService);
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

            assertEquals(sneakyFieldDeprecated, field.isDeprecated());
            assertEquals("Maybe hides its deprecation", field.getDescription());
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

        assertFalse(bunnyType.getValue("Omg").isDeprecated());
    }

    @SuppressWarnings("unused")
    public static class BoringService {
        @GraphQLQuery(deprecationReason = "Boring")
        public Blob staleQuery() {
            return null;
        }

        @GraphQLQuery
        public String freshQuery() {
            return "";
        }

        @GraphQLQuery(name = "sneakyQuery", description = "Maybe hides its deprecation")
        @Deprecated
        public String trickyQuery() {
            return "";
        }

        @GraphQLMutation(deprecationReason = "Boring")
        public void staleMutation() {
        }

        @GraphQLMutation
        public void freshMutation() {
        }

        @GraphQLMutation(name = "sneakyMutation", description = "Maybe hides its deprecation")
        @Deprecated
        public void trickyMutation() {
        }

        @GraphQLSubscription(deprecationReason = "Boring")
        public Publisher<String> staleSubscription() {
            return null;
        }

        @GraphQLSubscription
        public Publisher<String> freshSubscription() {
            return null;
        }

        @GraphQLSubscription(name = "sneakySubscription", description = "Maybe hides its deprecation")
        @Deprecated
        public Publisher<String> trickySubscription() {
            return null;
        }
    }

    @GraphQLType(name = "TheBlob", description = "Blip Bloop")
    static class Blob {}

    @SuppressWarnings("unused")
    public static class BunnyService {
        @GraphQLQuery
        public BunnyType bunnyType() {
            return BunnyType.ADORABLE;
        }
    }

    @SuppressWarnings("unused")
    @GraphQLType(description = "Types of bunnies")
    public enum BunnyType {
        @GraphQLEnumValue(name = "IntenselyCute") CUTE,
        @GraphQLEnumValue(description = "Thoroughly adorable") ADORABLE,
        @GraphQLEnumValue(deprecationReason = "Impossible") MEH,
        @Deprecated AVERAGE,
        @Deprecated @GraphQLEnumValue(name = "Omg") WOW
    }
}
