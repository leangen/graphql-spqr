package io.leangen.graphql.util;

import com.google.gson.Gson;

import java.lang.reflect.AnnotatedType;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.strategy.input.GsonInputDeserializer;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;

public class Scalars {

    public static GraphQLScalarType GraphQLUuid = new GraphQLScalarType("UUID", "Built-in UUID", new Coercing() {
        @Override
        public Object serialize(Object input) {
            if (input instanceof String || input instanceof UUID) {
                return input;
            } else {
                return null;
            }
        }

        @Override
        public Object parseValue(Object input) {
            return serialize(input);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (!(input instanceof StringValue)) return null;
            return UUID.fromString(((StringValue) input).getValue());
        }
    });

    public static GraphQLScalarType GraphQLUri = new GraphQLScalarType("URI", "Built-in URI", new Coercing() {
        @Override
        public Object serialize(Object input) {
            if (input instanceof URI) {
                return input.toString();
            } else if (input instanceof String) {
                return input;
            } else {
                return null;
            }
        }

        @Override
        public Object parseValue(Object input) {
            return serialize(input);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (!(input instanceof StringValue)) return null;
            return URI.create(((StringValue) input).getValue());
        }
    });

    public static GraphQLScalarType GraphQLISODate = new GraphQLScalarType("Date", "Built-in date", new Coercing() {
        private String toISODateString(Date date) {
            return date.toInstant().toString();
        }

        private Date fromISODateString(String isoDateString) {
            return new Date(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(isoDateString)).toEpochMilli());
        }

        @Override
        public Object serialize(Object input) {
            if (input instanceof Date) {
                return toISODateString(((Date) input));
            } else if (input instanceof Long) {
                return toISODateString(new Date((Long) input));
            } else if (input instanceof String) {
                return input;
            } else {
                return null;
            }
        }

        @Override
        public Object parseValue(Object input) {
            return serialize(input);
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof StringValue) {
                return fromISODateString(((StringValue) input).getValue());
            } else if (input instanceof IntValue) {
                return new Date(((IntValue) input).getValue().longValue());
            } else {
                return null;
            }
        }
    });

    public static GraphQLScalarType graphQLJson(AnnotatedType type) {
        return new GraphQLScalarType("JSON_" + type.getType().getTypeName(), "Built-in JSON", new Coercing() {

            private final InputDeserializer deserializer = new GsonInputDeserializer(new Gson());
            private final AnnotatedType MAP = GenericTypeReflector.annotate(Map.class);

            @Override
            public Object serialize(Object input) {
                return deserializer.deserialize(input, MAP);
            }

            @Override
            public Object parseValue(Object input) {
                return serialize(input);
            }

            @Override
            public Object parseLiteral(Object input) {
                if (!(input instanceof ObjectValue)) return null;
                return deserializer.deserialize(parseFieldValue(((ObjectValue) input)), type);
            }

            private Object parseFieldValue(Value value) {
                if (value instanceof StringValue) {
                    return ((StringValue) value).getValue();
                }
                if (value instanceof IntValue) {
                    return ((IntValue) value).getValue();
                }
                if (value instanceof FloatValue) {
                    return ((FloatValue) value).getValue();
                }
                if (value instanceof BooleanValue) {
                    return ((BooleanValue) value).isValue();
                }
                if (value instanceof EnumValue) {
                    ((EnumValue) value).getName();
                }
                if (value instanceof ArrayValue) {
                    return ((ArrayValue) value).getValues().stream()
                            .map(this::parseFieldValue)
                            .collect(Collectors.toList());
                }
                if (value instanceof ObjectValue) {
                    return ((ObjectValue) value).getObjectFields().stream()
                            .collect(Collectors.toMap(ObjectField::getName, field -> parseFieldValue(field.getValue())));
                }
                //Should never happen, as it would mean the variable was not replaced by the parser
                throw new IllegalArgumentException("Unsupported scalar value type: " + value.getClass().getName());
            }
        });
    }
}
