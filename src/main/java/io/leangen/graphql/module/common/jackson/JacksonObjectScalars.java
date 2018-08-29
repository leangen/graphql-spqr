package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.GraphQLScalarType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.leangen.graphql.util.Scalars.literalOrException;

@SuppressWarnings("WeakerAccess")
public class JacksonObjectScalars {

    public static final GraphQLScalarType JsonObjectNode = new GraphQLScalarType("JsonObject", "JSON object", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            return dataFetcherResult;
        }

        @Override
        public Object parseValue(Object input) {
            return input;
        }

        @Override
        public Object parseLiteral(Object input) {
            return parseJsonValue(literalOrException(input, ObjectValue.class));
        }
    });

    public static final GraphQLScalarType JsonAnyNode = new GraphQLScalarType("Json", "Any JSON value", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            GraphQLScalarType scalar = JacksonScalars.toGraphQLScalarType(dataFetcherResult.getClass());
            return scalar != null ? scalar.getCoercing().serialize(dataFetcherResult) : dataFetcherResult;
        }

        @Override
        public Object parseValue(Object input) {
            return input;
        }

        @Override
        public Object parseLiteral(Object input) {
            return parseJsonValue(((Value) input));
        }
    });

    private static JsonNode parseJsonValue(Value value) {
        if (value instanceof BooleanValue) {
            return JsonNodeFactory.instance.booleanNode(((BooleanValue) value).isValue());
        }
        if (value instanceof EnumValue) {
            return JsonNodeFactory.instance.textNode(((EnumValue) value).getName());
        }
        if (value instanceof FloatValue) {
            return JsonNodeFactory.instance.numberNode(((FloatValue) value).getValue());
        }
        if (value instanceof IntValue) {
            return JsonNodeFactory.instance.numberNode(((IntValue) value).getValue());
        }
        if (value instanceof NullValue) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (value instanceof StringValue) {
            return JsonNodeFactory.instance.textNode(((StringValue) value).getValue());
        }
        if (value instanceof ArrayValue) {
            List<Value> values = ((ArrayValue) value).getValues();
            ArrayNode jsonArray = JsonNodeFactory.instance.arrayNode(values.size());
            values.forEach(v -> jsonArray.add(parseJsonValue(v)));
            return jsonArray;
        }
        if (value instanceof ObjectValue) {
            final ObjectNode result = JsonNodeFactory.instance.objectNode();
            ((ObjectValue) value).getObjectFields().forEach(objectField ->
                    result.set(objectField.getName(), parseJsonValue(objectField.getValue())));
            return result;
        }
        //Should never happen, as it would mean the variable was not replaced by the parser
        throw new CoercingParseLiteralException("Unknown scalar AST type: " + value.getClass().getName());
    }

    private static final Map<Type, GraphQLScalarType> SCALAR_MAPPING = getScalarMapping();

    public static boolean isScalar(Type javaType) {
        return SCALAR_MAPPING.containsKey(javaType);
    }

    public static GraphQLScalarType toGraphQLScalarType(Type javaType) {
        return SCALAR_MAPPING.get(javaType);
    }

    private static Map<Type, GraphQLScalarType> getScalarMapping() {
        Map<Type, GraphQLScalarType> scalarMapping = new HashMap<>();
        scalarMapping.put(ObjectNode.class, JsonObjectNode);
        scalarMapping.put(POJONode.class, JsonObjectNode);
        scalarMapping.put(JsonNode.class, JsonAnyNode);
        return Collections.unmodifiableMap(scalarMapping);
    }
}
