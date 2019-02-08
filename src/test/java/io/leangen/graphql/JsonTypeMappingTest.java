package io.leangen.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.domain.Street;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import org.eclipse.microprofile.graphql.Query;
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

        @Query
        public JacksonContainer item(JacksonContainer in) {
            return in;
        }
    }

    public static class JacksonContainer {

        private ObjectNode obj;
        private JsonNode any;
        private BinaryNode binary;
        private TextNode text;
        private IntNode integer;
        private DoubleNode dbl;
        private BigIntegerNode bigInt;
        private ArrayNode array;

        public JacksonContainer(ObjectNode obj, JsonNode any, BinaryNode binary, TextNode text, IntNode integer,
                                DoubleNode dbl, BigIntegerNode bigInt, ArrayNode array) {
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

        @Query
        public GsonContainer item(GsonContainer in) {
            return in;
        }
    }

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
