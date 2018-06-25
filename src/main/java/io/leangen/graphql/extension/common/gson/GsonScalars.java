package io.leangen.graphql.extension.common.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import static io.leangen.graphql.util.Scalars.literalParsingException;
import static io.leangen.graphql.util.Scalars.serializationException;
import static io.leangen.graphql.util.Scalars.valueParsingException;

public class GsonScalars {

    public static final GraphQLScalarType JsonAnyNode = new GraphQLScalarType("JSON", "JSON object", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof JsonPrimitive) {
                return JsonPrimitiveNode.getCoercing().serialize(dataFetcherResult);
            }
            return dataFetcherResult;
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

    public static final GraphQLScalarType JsonObjectNode = new GraphQLScalarType("JSONObject", "JSON object", new Coercing() {

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

    public static final GraphQLScalarType JsonPrimitiveNode = new GraphQLScalarType("JSONPrimitive", "A primitive JSON value", new Coercing() {

        @Override
        public Object serialize(Object dataFetcherResult) {
            if (dataFetcherResult instanceof JsonPrimitive) {
                JsonPrimitive primitive = (JsonPrimitive) dataFetcherResult;
                if (primitive.isString()) {
                    return primitive.getAsString();
                }
                if (primitive.isNumber()) {
                    return primitive.getAsNumber();
                }
                if (primitive.isBoolean()) {
                    return primitive.getAsBoolean();
                }
                if (primitive.isJsonNull()) {
                    return null;
                }
            }
            throw serializationException(dataFetcherResult, JsonPrimitive.class);
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof String) {
                return new JsonPrimitive((String) input);
            }
            if (input instanceof Number) {
                return new JsonPrimitive((Number) input);
            }
            if (input instanceof Boolean) {
                return new JsonPrimitive((Boolean) input);
            }
            if (input instanceof Character) {
                return new JsonPrimitive((Character) input);
            }
            throw valueParsingException(input, String.class, Number.class, Boolean.class, Character.class);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof ObjectValue || input instanceof ArrayValue) {
                throw literalParsingException(input, StringValue.class, BooleanValue.class, EnumValue.class,
                        FloatValue.class, IntValue.class, NullValue.class);
            }
            return parseJsonValue(((Value) input));
        }
    });

    private static JsonElement parseJsonValue(Value value) {
        if (value instanceof BooleanValue) {
            return new JsonPrimitive(((BooleanValue) value).isValue());
        }
        if (value instanceof EnumValue) {
            return new JsonPrimitive(((EnumValue) value).getName());
        }
        if (value instanceof FloatValue) {
            return new JsonPrimitive(((FloatValue) value).getValue());
        }
        if (value instanceof IntValue) {
            return new JsonPrimitive(((IntValue) value).getValue());
        }
        if (value instanceof NullValue) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof StringValue) {
            return new JsonPrimitive(((StringValue) value).getValue());
        }
        if (value instanceof ArrayValue) {
            List<Value> values = ((ArrayValue) value).getValues();
            JsonArray jsonArray = new JsonArray(values.size());
            values.forEach(v -> jsonArray.add(parseJsonValue(v)));
            return jsonArray;
        }
        if (value instanceof ObjectValue) {
            final JsonObject result = new JsonObject();
            ((ObjectValue) value).getObjectFields().forEach(objectField ->
                    result.add(objectField.getName(), parseJsonValue(objectField.getValue())));
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
        scalarMapping.put(JsonObject.class, JsonObjectNode);
        scalarMapping.put(JsonElement.class, JsonAnyNode);
        scalarMapping.put(JsonPrimitive.class, JsonPrimitiveNode);
        return Collections.unmodifiableMap(scalarMapping);
    }
}
