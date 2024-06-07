package io.leangen.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.*;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLId;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.domain.Education;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.union.Union2;
import io.leangen.graphql.support.TestLog;
import io.leangen.graphql.util.GraphQLUtils;
import io.leangen.graphql.util.Urls;
import org.junit.Test;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.Map;

import static graphql.schema.GraphQLObjectType.newObject;
import static io.leangen.graphql.support.LogAssertions.assertWarningsLogged;
import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
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
    public void additionalTypesFullCopyTest() {
        ExecutableSchema executableSchema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .generateExecutable();
        GraphQLSchema schema = executableSchema.getSchema();

        GraphQLSchema schema2 = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .withAdditionalTypes(
                        schema.getAllTypesAsList(),
                        executableSchema.getTypeRegistry().getMappedTypes(),
                        schema.getCodeRegistry())
                .generate();

        schema.getTypeMap().values().stream()
                .filter(type -> !GraphQLUtils.isIntrospectionType(type) && type != schema.getQueryType())
                .forEach(type -> {
                    assertTrue(schema2.getTypeMap().containsKey(type.getName()));
                    GraphQLType type2 = schema2.getTypeMap().get(type.getName());
                    assertSameType(type, type2, schema.getCodeRegistry(), schema2.getCodeRegistry());
                });

        GraphQL exe = GraphQL.newGraphQL(schema2).build();
        ExecutionResult result = exe.execute("{mix(id: \"1\") {... on Street {name}}}");
        assertNoErrors(result);
    }

    @Test
    public void additionalTypesPartialCopyTest() {
        ExecutableSchema executableSchema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new PersonService())
                .withAdditionalDirectives(Directive.class)
                .generateExecutable();
        GraphQLSchema schema = executableSchema.getSchema();
        Map<String, AnnotatedType> mappedTypes = executableSchema.getTypeRegistry().getMappedTypes();
        GraphQLSchema schema2 = new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .withAdditionalTypes(Collections.singleton(schema.getType("Person")), mappedTypes, schema.getCodeRegistry())
                .withAdditionalDirectives(schema.getDirective("directive"))
                .generate();

        assertSameType(schema.getType("Address"), schema2.getType("Address"),
                schema.getCodeRegistry(), schema2.getCodeRegistry());
    }

    @Test(expected = ConfigurationException.class)
    public void additionalTypesCollisionTest() {
        ExecutableSchema executableSchema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new PersonService())
                .generateExecutable();
        GraphQLSchema schema = executableSchema.getSchema();
        Map<String, AnnotatedType> mappedTypes = executableSchema.getTypeRegistry().getMappedTypes();
        new TestSchemaGenerator()
                .withOperationsFromSingleton(new Service())
                .withAdditionalTypes(Collections.singleton(schema.getType("Person")), mappedTypes, schema.getCodeRegistry())
                .withAdditionalTypes(Collections.singleton(newObject().name("Street").build()), mappedTypes, schema.getCodeRegistry())
                .generate();
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

    private void assertSameType(GraphQLType t1, GraphQLType t2, GraphQLCodeRegistry code1, GraphQLCodeRegistry code2) {
        assertSame(t1, t2);
        if (t1 instanceof GraphQLInterfaceType) {
            GraphQLInterfaceType i = (GraphQLInterfaceType) t1;
            assertSame(code1.getTypeResolver(i), code2.getTypeResolver(i));
        }
        if (t1 instanceof GraphQLUnionType) {
            GraphQLUnionType u = (GraphQLUnionType) t1;
            assertSame(code1.getTypeResolver(u), code2.getTypeResolver(u));
        }
        if (t1 instanceof GraphQLFieldsContainer) {
            GraphQLFieldsContainer c = (GraphQLFieldsContainer) t1;
            c.getFieldDefinitions().forEach(fieldDef ->
                    assertSame(code1.getDataFetcher(FieldCoordinates.coordinates(c, fieldDef), fieldDef), code2.getDataFetcher(FieldCoordinates.coordinates(c, fieldDef), fieldDef)));
        }
    }

    public static class Service {

        @GraphQLQuery(name = "street")
        public Street getStreet() {
            return null;
        }

        @GraphQLQuery
        public Company company(Company in) {
            return new Company();
        }

        @GraphQLQuery
        public @GraphQLUnion(name = "mix") Union2<Street, Education> mix(@GraphQLEnvironment ResolutionEnvironment env, @GraphQLId(relayId = true) int id) {
            GraphQLOutputType mixType = env.globalEnvironment.typeRegistry.getOutputTypes("mix").get(0).graphQLType;
            assertTrue(mixType instanceof GraphQLObjectType);
            return null;
        }
    }

    @GraphQLInterface(name = "Addressable")
    public interface Addressable {
        Address getAddress();
    }

    public static class Company implements Addressable {

        private Address address;

        @Override
        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        public Street street;
        public int block;
    }

    public static class Person {
        public Address address;
        public Person spouse;
    }

    public static class Directive {
        public Company company;
    }

    public static class PersonService {
        @GraphQLQuery
        public Person person(Person in) {
            return new Person();
        }
    }

    private static class Foo {}
    private static class FooInput {}

    private static class NameCollision {
        @GraphQLQuery
        public FooInput test(Foo foo, FooInput fooInput) {return fooInput;}
    }
}
