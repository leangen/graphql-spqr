package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.union.Union2;
import io.leangen.graphql.support.TestLog;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Urls;
import org.junit.Test;

import static io.leangen.graphql.support.LogAssertions.assertWarningsLogged;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TypeRegistryTest {

    @Test
    public void referenceReplacementTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .generate();

        GraphQL exe = GraphQL.newGraphQL(schema).build();
        ExecutionResult result = exe.execute("{mix(id: \"1\") {... on Street {name}}}");
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void knownTypesTest() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .generate();

        GraphQLSchema schema2 = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .withAdditionalTypes(schema.getAllTypesAsList())
                .generate();

        schema.getTypeMap().entrySet().stream()
                .filter(entry -> !GraphQLUtils.isIntrospectionType(entry.getValue()) && entry.getValue() != schema.getQueryType())
                .forEach(entry -> {
                    assertTrue(schema2.getTypeMap().containsKey(entry.getKey()));
                    assertSame(entry.getValue(), schema2.getTypeMap().get(entry.getKey()));
                });

        GraphQL exe = GraphQL.newGraphQL(schema2).build();
        ExecutionResult result = exe.execute("{mix(id: \"1\") {... on Street {name}}}");
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void nameUniquenessTest() {
        try (TestLog log = new TestLog(OperationMapper.class)) {
            try {
                new TestSchemaGenerator()
                        .withOperationsFromSingleton(new NameCollision())
                        .generate();
            } catch (Exception e) {/*Ignore exceptions from graphql-java, only testing validation in SPQR*/}
            assertWarningsLogged(log.getEvents(), Urls.Errors.NON_UNIQUE_TYPE_NAME);
        }
    }

    public static class Service {

        @GraphQLQuery(name = "street")
        public Street getStreet() {
            return null;
        }

        @GraphQLQuery
        public @GraphQLUnion(name = "mix") Union2<Street, Education> mix(@GraphQLEnvironment ResolutionEnvironment env, @GraphQLId(relayId = true) int id) {
            GraphQLOutputType mixType = env.globalEnvironment.typeRegistry.getOutputTypes("mix").get(0).graphQLType;
            assertTrue(mixType instanceof GraphQLObjectType);
            return null;
        }
    }

    private static class Foo {}
    private static class FooInput {}

    public static class NameCollision {
        @GraphQLQuery
        public FooInput test(Foo foo, FooInput fooInput) {return fooInput;}
    }
}
