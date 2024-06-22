package io.leangen.graphql;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
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
        ExecutionResult result = exe.execute( "{item(in: {" +
                "  obj: {one: \"two\"}," +
                "  any: 3.3," +
                "  binary: \"UmFuZG9tIGp1bms=\"," +
                "  text: \"some text\"," +
                "  integer: 123," +
                "  dbl: 12.123," +
                "  bigInt: 99999999999," +
                "  array: [333, {one: \"two\"}]}) {" +
                "    obj, any, text, binary, integer, dbl, bigInt, array, pojo" +
                "  }" +
                "}");
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
        private final TextNode text;
        private final IntNode integer;
        private final DoubleNode dbl;
        private final BigIntegerNode bigInt;
        private final ArrayNode array;

        public JacksonContainer(@JsonProperty("obj") ObjectNode obj,
                                @JsonProperty("any") JsonNode any,
                                @JsonProperty("binary") BinaryNode binary,
                                @JsonProperty("text") TextNode text,
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

        public TextNode getText() {
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
