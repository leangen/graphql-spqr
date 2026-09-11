package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson2.JacksonValueMapperFactory;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;

import static io.leangen.graphql.support.QueryResultAssertions.assertNoErrors;
import static io.leangen.graphql.support.QueryResultAssertions.assertTypeAtPathIs;

public class JsonTypeMappingTest {

    @Test
    public void testJacksonTypeMapping() {
        GraphQLSchemaGenerator gen = new TestSchemaGenerator()
                .withOperationsFromSingleton(new JacksonService());
        GraphQLSchema schema = gen.generate();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();
        ExecutionResult result = exe.execute(jacksonQuery());
        assertNoErrors(result);
        assertTypeAtPathIs(ObjectNode.class, result, "item.obj");
        assertTypeAtPathIs(POJONode.class, result, "item.pojo");
        assertTypeAtPathIs(Number.class, result, "item.any");
        assertTypeAtPathIs(String.class, result, "item.binary");
        assertTypeAtPathIs(String.class, result, "item.text");
        assertTypeAtPathIs(Integer.class, result, "item.integer");
        assertTypeAtPathIs(Double.class, result, "item.dbl");
        assertTypeAtPathIs(BigInteger.class, result, "item.bigInt");
        assertTypeAtPathIs(List.class, result, "item.array");
        assertTypeAtPathIs(BigInteger.class, result, "item.array.0");
        assertTypeAtPathIs(ObjectNode.class, result, "item.array.1");
    }

    @Test
    public void testJackson2TypeMapping() {
        GraphQLSchema schema = new TestSchemaGenerator()
                .withValueMapperFactory(new JacksonValueMapperFactory())
                .withModules(new io.leangen.graphql.module.common.jackson2.JacksonModule())
                .withOperationsFromSingleton(new Jackson2Service())
                .generate();

        ExecutionResult result = GraphQLRuntime.newGraphQL(schema).build().execute(jacksonQuery());
        assertNoErrors(result);
        assertTypeAtPathIs(com.fasterxml.jackson.databind.node.ObjectNode.class, result, "item.obj");
        assertTypeAtPathIs(com.fasterxml.jackson.databind.node.POJONode.class, result, "item.pojo");
        assertTypeAtPathIs(Number.class, result, "item.any");
        assertTypeAtPathIs(String.class, result, "item.binary");
        assertTypeAtPathIs(String.class, result, "item.text");
        assertTypeAtPathIs(Integer.class, result, "item.integer");
        assertTypeAtPathIs(Double.class, result, "item.dbl");
        assertTypeAtPathIs(BigInteger.class, result, "item.bigInt");
        assertTypeAtPathIs(List.class, result, "item.array");
        assertTypeAtPathIs(BigInteger.class, result, "item.array.0");
        assertTypeAtPathIs(com.fasterxml.jackson.databind.node.ObjectNode.class, result, "item.array.1");
    }

    private static String jacksonQuery() {
        return "{item(in: {"
                + "  obj: {one: \"two\"},"
                + "  any: 3.3,"
                + "  binary: \"UmFuZG9tIGp1bms=\","
                + "  text: \"some text\","
                + "  integer: 123,"
                + "  dbl: 12.123,"
                + "  bigInt: 99999999999,"
                + "  array: [333, {one: \"two\"}]}) {"
                + "    obj, any, text, binary, integer, dbl, bigInt, array, pojo"
                + "  }"
                + "}";
    }

    @Test
    public void testGsonTypeMapping() {
        GraphQLSchemaGenerator gen = new TestSchemaGenerator()
                .withValueMapperFactory(new GsonValueMapperFactory())
                .withOperationsFromSingleton(new GsonService());
        GraphQLSchema schema = gen.generate();

        GraphQL exe = GraphQLRuntime.newGraphQL(schema).build();
        ExecutionResult result = exe.execute( "{item(in: {" +
                "  obj: {one: \"two\"}," +
                "  any: 3.3," +
                "  primitive: 123," +
                "  array: [{one: \"two\"}, 3.3, [123]]}) {" +
                "    obj, any, primitive, array" +
                "  }" +
                "}");
        assertNoErrors(result);
        assertTypeAtPathIs(JsonObject.class, result, "item.obj");
        assertTypeAtPathIs(Number.class, result, "item.any");
        assertTypeAtPathIs(Number.class, result, "item.primitive");
        assertTypeAtPathIs(List.class, result, "item.array");
        assertTypeAtPathIs(JsonObject.class, result, "item.array.0");
        assertTypeAtPathIs(Number.class, result, "item.array.1");
    }

    public static class JacksonService {

        @GraphQLQuery
        public JacksonContainer item(JacksonContainer in) {
            return in;
        }
    }

    @SuppressWarnings("unused")
    public static class JacksonContainer {

        private final ObjectNode obj;
        private final JsonNode any;
        private final BinaryNode binary;
        private final StringNode text;
        private final IntNode integer;
        private final DoubleNode dbl;
        private final BigIntegerNode bigInt;
        private final ArrayNode array;

        public JacksonContainer(@JsonProperty("obj") ObjectNode obj,
                                @JsonProperty("any") JsonNode any,
                                @JsonProperty("binary") BinaryNode binary,
                                @JsonProperty("text") StringNode text,
                                @JsonProperty("integer") IntNode integer,
                                @JsonProperty("dbl") DoubleNode dbl,
                                @JsonProperty("bigInt") BigIntegerNode bigInt,
                                @JsonProperty("array") ArrayNode array) {
            this.obj = obj;
            this.any = any;
            this.text = text;
            this.binary = binary;
            this.integer = integer;
            this.dbl = dbl;
            this.bigInt = bigInt;
            this.array = array;
        }

        public ObjectNode getObj() {
            return obj;
        }

        public JsonNode getAny() {
            return any;
        }

        public BinaryNode getBinary() {
            return binary;
        }

        public StringNode getText() {
            return text;
        }

        public IntNode getInteger() {
            return integer;
        }

        public DoubleNode getDbl() {
            return dbl;
        }

        public BigIntegerNode getBigInt() {
            return bigInt;
        }

        public POJONode getPojo() {
            return new POJONode(new Street("Fake Street", 123));
        }

        public ArrayNode getArray() {
            return array;
        }
    }

    public static class GsonService {

        @GraphQLQuery
        public GsonContainer item(GsonContainer in) {
            return in;
        }
    }

    public static class Jackson2Service {

        @GraphQLQuery
        public Jackson2Container item(Jackson2Container in) {
            return in;
        }
    }

    @SuppressWarnings("unused")
    public static class Jackson2Container {

        private final com.fasterxml.jackson.databind.node.ObjectNode obj;
        private final com.fasterxml.jackson.databind.JsonNode any;
        private final com.fasterxml.jackson.databind.node.BinaryNode binary;
        private final com.fasterxml.jackson.databind.node.TextNode text;
        private final com.fasterxml.jackson.databind.node.IntNode integer;
        private final com.fasterxml.jackson.databind.node.DoubleNode dbl;
        private final com.fasterxml.jackson.databind.node.BigIntegerNode bigInt;
        private final com.fasterxml.jackson.databind.node.ArrayNode array;

        public Jackson2Container(@JsonProperty("obj") com.fasterxml.jackson.databind.node.ObjectNode obj,
                                 @JsonProperty("any") com.fasterxml.jackson.databind.JsonNode any,
                                 @JsonProperty("binary") com.fasterxml.jackson.databind.node.BinaryNode binary,
                                 @JsonProperty("text") com.fasterxml.jackson.databind.node.TextNode text,
                                 @JsonProperty("integer") com.fasterxml.jackson.databind.node.IntNode integer,
                                 @JsonProperty("dbl") com.fasterxml.jackson.databind.node.DoubleNode dbl,
                                 @JsonProperty("bigInt") com.fasterxml.jackson.databind.node.BigIntegerNode bigInt,
                                 @JsonProperty("array") com.fasterxml.jackson.databind.node.ArrayNode array) {
            this.obj = obj;
            this.any = any;
            this.binary = binary;
            this.text = text;
            this.integer = integer;
            this.dbl = dbl;
            this.bigInt = bigInt;
            this.array = array;
        }

        public com.fasterxml.jackson.databind.node.ObjectNode getObj() { return obj; }
        public com.fasterxml.jackson.databind.JsonNode getAny() { return any; }
        public com.fasterxml.jackson.databind.node.BinaryNode getBinary() { return binary; }
        public com.fasterxml.jackson.databind.node.TextNode getText() { return text; }
        public com.fasterxml.jackson.databind.node.IntNode getInteger() { return integer; }
        public com.fasterxml.jackson.databind.node.DoubleNode getDbl() { return dbl; }
        public com.fasterxml.jackson.databind.node.BigIntegerNode getBigInt() { return bigInt; }
        public com.fasterxml.jackson.databind.node.POJONode getPojo() { return new com.fasterxml.jackson.databind.node.POJONode(new Street("Fake Street", 123)); }
        public com.fasterxml.jackson.databind.node.ArrayNode getArray() { return array; }
    }

    @SuppressWarnings("unused")
    public static class GsonContainer {

        private JsonPrimitive primitive;
        private JsonObject obj;
        private JsonElement any;
        private JsonArray array;

        public JsonPrimitive getPrimitive() {
            return primitive;
        }

        public JsonObject getObj() {
            return obj;
        }

        public JsonElement getAny() {
            return any;
        }

        public JsonArray getArray() {
            return array;
        }
    }
}
