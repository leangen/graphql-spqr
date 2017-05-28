package io.leangen.graphql.util;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;

import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBigInteger;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLByte;
import static graphql.Scalars.GraphQLChar;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLShort;
import static graphql.Scalars.GraphQLString;

@SuppressWarnings("WeakerAccess")
public class Scalars {

    public static final GraphQLNonNull RelayId = new GraphQLNonNull(graphql.Scalars.GraphQLID);
    
    public static final GraphQLScalarType GraphQLUuid = new GraphQLScalarType("UUID", "Built-in UUID", new Coercing() {
        @Override
        public Object serialize(Object input) {
            if (input instanceof String) {
                return input;
            } if (input instanceof UUID) {
                return input.toString();
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

    public static final GraphQLScalarType GraphQLUri = new GraphQLScalarType("URI", "Built-in URI", new Coercing() {
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

    public static final GraphQLScalarType GraphQLLocale = new GraphQLScalarType("Locale", "Built-in locale", new Coercing() {
        @Override
        public Object serialize(Object input) {
            if (input instanceof Locale) {
                return ((Locale) input).toLanguageTag();
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
            return Locale.forLanguageTag(((StringValue) input).getValue());
        }
    });
    
    public static final GraphQLScalarType GraphQLISODate = new GraphQLScalarType("Date", "Built-in date", new Coercing() {
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

    public static GraphQLScalarType graphQLObjectScalar(String name) {
        return new GraphQLScalarType(name, "Built-in object scalar", new Coercing() {

            @Override
            public Object serialize(Object input) {
                return input;
            }

            @Override
            public Object parseValue(Object input) {
                return serialize(input);
            }

            @Override
            public Object parseLiteral(Object input) {
                if (!(input instanceof ObjectValue)) return null;
                return parseFieldValue(((ObjectValue) input));
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

    private static final Map<Type, GraphQLScalarType> SCALAR_MAPPING = getScalarMapping();

    public static boolean isScalar(Type javaType) {
        return SCALAR_MAPPING.containsKey(javaType);
    }

    public static GraphQLScalarType toGraphQLScalarType(Type javaType) {
        return SCALAR_MAPPING.get(javaType);
    }
    
    private static Map<Type, GraphQLScalarType> getScalarMapping() {
        Map<Type, GraphQLScalarType> scalarMapping = new HashMap<>();
        scalarMapping.put(Character.class, GraphQLChar);
        scalarMapping.put(char.class, GraphQLChar);
        scalarMapping.put(String.class, GraphQLString);
        scalarMapping.put(Byte.class, GraphQLByte);
        scalarMapping.put(byte.class, GraphQLByte);
        scalarMapping.put(Short.class, GraphQLShort);
        scalarMapping.put(short.class, GraphQLShort);
        scalarMapping.put(Integer.class, GraphQLInt);
        scalarMapping.put(int.class, GraphQLInt);
        scalarMapping.put(Long.class, GraphQLLong);
        scalarMapping.put(long.class, GraphQLLong);
        scalarMapping.put(Float.class, GraphQLFloat);
        scalarMapping.put(float.class, GraphQLFloat);
        scalarMapping.put(Double.class, GraphQLFloat);
        scalarMapping.put(double.class, GraphQLFloat);
        scalarMapping.put(BigInteger.class, GraphQLBigInteger);
        scalarMapping.put(BigDecimal.class, GraphQLBigDecimal);
        scalarMapping.put(Number.class, GraphQLBigDecimal);
        scalarMapping.put(Boolean.class, GraphQLBoolean);
        scalarMapping.put(boolean.class, GraphQLBoolean);
        scalarMapping.put(UUID.class, GraphQLUuid);
        scalarMapping.put(URI.class, GraphQLUri);
        scalarMapping.put(Date.class, GraphQLISODate);
        scalarMapping.put(Locale.class, GraphQLLocale);
        return Collections.unmodifiableMap(scalarMapping);
    }
}
