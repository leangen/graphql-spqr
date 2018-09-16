package io.leangen.graphql;

import graphql.DirectivesUtil;
import graphql.Scalars;
import graphql.schema.GraphQLDirectiveContainer;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.annotations.types.GraphQLDirective;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DirectiveTest {

    @Test
    public void testDirectives() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withOperationsFromSingleton(new ServiceWithDirectives())
                .generate();

        GraphQLFieldDefinition scalarField = schema.getQueryType().getFieldDefinition("scalar");
        assertDirective(scalarField, "fieldDef", "fieldDef");

        GraphQLScalarType scalarResult = (GraphQLScalarType) scalarField.getType();
        assertDirective(scalarResult, "scalar", "scalar");

        graphql.schema.GraphQLArgument argument = scalarField.getArgument("in");
        assertDirective(argument, "argDef", "argument");

        GraphQLInputObjectType inputType = (GraphQLInputObjectType) argument.getType();
        assertDirective(inputType, "inputObjectType", "input");
        graphql.schema.GraphQLArgument directiveArg = DirectivesUtil.directiveWithArg(inputType.getDirectives(), "inputObjectType", "value").get();
        Optional<graphql.schema.GraphQLArgument> metaArg = DirectivesUtil.directiveWithArg(directiveArg.getDirectives(), "meta", "value");
        assertTrue(metaArg.isPresent());
        assertEquals("meta", metaArg.get().getValue());

        GraphQLInputObjectField inputField = inputType.getField("value");
        assertDirective(inputField, "inputFieldDef", "inputField");

        GraphQLFieldDefinition objField = schema.getQueryType().getFieldDefinition("obj");
        GraphQLObjectType objResult = (GraphQLObjectType) objField.getType();
        assertDirective(objResult, "objectType", "object");

        GraphQLFieldDefinition innerField = objResult.getFieldDefinition("value");
        assertDirective(innerField, "fieldDef", "field");
    }

    private void assertDirective(GraphQLDirectiveContainer container, String directiveName, String innerName) {
        Optional<graphql.schema.GraphQLArgument> argument = DirectivesUtil.directiveWithArg(container.getDirectives(), directiveName, "value");
        assertTrue(argument.isPresent());
        GraphQLInputObjectType argType = (GraphQLInputObjectType) argument.get().getType();
        assertEquals("WrapperInput", argType.getName());
        assertSame(Scalars.GraphQLString, argType.getFieldDefinition("name").getType());
        assertSame(Scalars.GraphQLString, argType.getFieldDefinition("value").getType());
        Wrapper wrapper = (Wrapper) argument.get().getValue();
        assertEquals(innerName, wrapper.name());
        assertEquals("test", wrapper.value());
    }

    private static class ServiceWithDirectives {

        @GraphQLQuery
        @FieldDef(@Wrapper(name = "fieldDef", value = "test"))
        public @GraphQLScalar ScalarResult scalar(@GraphQLArgument(name = "in") @ArgDef(@Wrapper(name = "argument", value = "test")) Input in) {
            return null;
        }

        @GraphQLQuery
        @FieldDef(@Wrapper(name = "fieldDef", value = "test"))
        public ObjectResult obj(@GraphQLArgument(name = "in") @ArgDef(@Wrapper(name = "argument", value = "test")) String in) {
            return null;
        }
    }

    @Scalar(@Wrapper(name = "scalar", value = "test"))
    private static class ScalarResult {
        public String value;
    }

    @ObjectType(@Wrapper(name = "object", value = "test"))
    private static class ObjectResult {
        @FieldDef(@Wrapper(name = "field", value = "test"))
        @GraphQLQuery
        public String value;
    }

    @InputObjectType(@Wrapper(name = "input", value = "test"))
    private static class Input {
        @InputFieldDef(@Wrapper(name = "inputField", value = "test"))
        public String value;
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Scalar {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface ObjectType {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface FieldDef {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface ArgDef {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface InterfaceType {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface UnionType {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface EnumType {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface EnumValue {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface InputObjectType {
        @Meta("meta") Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface InputFieldDef {
        Wrapper value();
    }

    @GraphQLDirective
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Meta {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.ANNOTATION_TYPE})
    public @interface Wrapper {
        String name();
        String value();
    }
}
