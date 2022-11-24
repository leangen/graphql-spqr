package io.leangen.graphql.util;

import graphql.GraphQLException;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.language.VariableReference;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;
import io.leangen.geantyref.GenericTypeReflector;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.scalars.ExtendedScalars.GraphQLBigDecimal;
import static graphql.scalars.ExtendedScalars.GraphQLBigInteger;
import static graphql.scalars.ExtendedScalars.GraphQLByte;
import static graphql.scalars.ExtendedScalars.GraphQLChar;
import static graphql.scalars.ExtendedScalars.GraphQLLong;
import static graphql.scalars.ExtendedScalars.GraphQLShort;

@SuppressWarnings("WeakerAccess")
public class Scalars {

    public static final GraphQLNonNull RelayId = new GraphQLNonNull(graphql.Scalars.GraphQLID);

    public static final GraphQLScalarType GraphQLUuid = GraphQLScalarType.newScalar()
            .name("UUID")
            .description("UUID String")
            .coercing(new Coercing<UUID, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof UUID) {
                        return dataFetcherResult.toString();
                    }
                    if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    }
                    throw serializationException(dataFetcherResult, UUID.class, String.class);
                }

                @Override
                public UUID parseValue(Object input) {
                    if (input instanceof String) {
                        try {
                            return UUID.fromString((String) input);
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseValueException("Value \"" + input + "\" could not be parsed into a UUID", e);
                        }
                    }
                    if (input instanceof UUID) {
                        return (UUID) input;
                    }
                    throw valueParsingException(input, String.class, UUID.class);
                }

                @Override
                public UUID parseLiteral(Object input) {
                    StringValue string = literalOrException(input, StringValue.class);
                    try {
                        return UUID.fromString(string.getValue());
                    } catch (IllegalArgumentException e) {
                        throw new CoercingParseValueException("Value \"" + input + "\" could not be parsed into a UUID", e);
                    }
                }
            }).build();

    public static final GraphQLScalarType GraphQLUri = GraphQLScalarType.newScalar()
            .name("URI")
            .description("URI String")
            .coercing(new Coercing<URI, String>() {
                @Override
                public String  serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof URI) {
                        return dataFetcherResult.toString();
                    } else if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    } else {
                        throw serializationException(dataFetcherResult, String.class, URI.class);
                    }
                }

                @Override
                public URI parseValue(Object input) {
                    if (input instanceof String) {
                        try {
                            return URI.create((String) input);
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseValueException("Value \"" + input + "\" could not be parsed into a URI", e);
                        }
                    }
                    if (input instanceof URI) {
                        return (URI) input;
                    }
                    throw valueParsingException(input, String.class, URI.class);
                }

                @Override
                public URI parseLiteral(Object input) {
                    StringValue string = literalOrException(input, StringValue.class);
                    try {
                        return URI.create(string.getValue());
                    } catch (IllegalArgumentException e) {
                        throw new CoercingParseValueException("Value \"" + input + "\" could not be parsed into a URI", e);
                    }
                }
            }).build();

    public static final GraphQLScalarType GraphQLUrl = GraphQLScalarType.newScalar()
            .name("URL")
            .description("URL String")
            .coercing(new Coercing<URL, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof URL) {
                        return dataFetcherResult.toString();
                    } else if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    } else {
                        throw serializationException(dataFetcherResult, String.class, URL.class);
                    }
                }

                @Override
                public URL parseValue(Object input) {
                    if (input instanceof String) {
                        try {
                            return new URL((String) input);
                        } catch (MalformedURLException e) {
                            throw new CoercingParseValueException("Input string \"" + input + "\" could not be parsed into a URL", e);
                        }
                    }
                    if (input instanceof URL) {
                        return (URL) input;
                    }
                    throw valueParsingException(input, String.class, URL.class);
                }

                @Override
                public URL parseLiteral(Object input) {
                    StringValue string = literalOrException(input, StringValue.class);
                    try {
                        return new URL(string.getValue());
                    } catch (MalformedURLException e) {
                        throw new CoercingParseLiteralException("Input string \"" + input + "\" could not be parsed into a URL", e);
                    }
                }
            }).build();

    public static final GraphQLScalarType GraphQLBase64String = GraphQLScalarType.newScalar()
            .name("Base64String")
            .description("Base64-encoded binary")
            .coercing(new Coercing<byte[], String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof byte[]) {
                        return Base64.getEncoder().encodeToString((byte[]) dataFetcherResult);
                    } else if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    } else {
                        throw serializationException(dataFetcherResult, String.class, byte[].class);
                    }
                }

                @Override
                public byte[] parseValue(Object input) {
                    if (input instanceof String) {
                        try {
                            return Base64.getDecoder().decode((String) input);
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseValueException("Input string \"" + input + "\" is not a valid Base64 value", e);
                        }
                    }
                    if (input instanceof byte[]) {
                        return (byte[]) input;
                    }
                    throw valueParsingException(input, String.class, byte[].class);
                }

                @Override
                public byte[] parseLiteral(Object input) {
                    StringValue string = literalOrException(input, StringValue.class);
                    try {
                        return Base64.getDecoder().decode(string.getValue());
                    } catch (IllegalArgumentException e) {
                        throw new CoercingParseLiteralException("Input string \"" + input + "\" is not a valid Base64 value", e);
                    }
                }
            }).build();

    public static final GraphQLScalarType GraphQLClass = GraphQLScalarType.newScalar()
            .name("Class")
            .description("A fully qualified class name")
            .coercing(new Coercing<Object, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof Class) {
                        return ((Class) dataFetcherResult).getName();
                    } else if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    } else {
                        throw serializationException(dataFetcherResult, String.class, Class.class);
                    }
                }

                @Override
                public Object parseValue(Object input) {
                    throw new CoercingParseValueException("Class can not be used as input");
                }

                @Override
                public Object parseLiteral(Object input) {
                    throw new CoercingParseLiteralException("Class can not be used as input");
                }
            }).build();

    public static final GraphQLScalarType GraphQLLocale = GraphQLScalarType.newScalar()
            .name("Locale")
            .description("Built-in Locale")
            .coercing(new Coercing<Locale, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof Locale) {
                        return ((Locale) dataFetcherResult).toLanguageTag();
                    } else if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    } else {
                        throw serializationException(dataFetcherResult, String.class, Locale.class);
                    }
                }

                @Override
                public Locale parseValue(Object input) {
                    if (input instanceof String) {
                        return Locale.forLanguageTag((String) input);
                    }
                    if (input instanceof Locale) {
                        return (Locale) input;
                    }
                    throw valueParsingException(input, String.class, Locale.class);
                }

                @Override
                public Locale parseLiteral(Object input) {
                    StringValue string = literalOrException(input, StringValue.class);
                    return Locale.forLanguageTag(string.getValue());
                }
            }).build();

    public static final GraphQLScalarType GraphQLDate = temporalScalar(Date.class,"Date", "an instant in time",
            s -> new Date(Instant.parse(s).toEpochMilli()), i -> new Date(i.toEpochMilli()), Scalars::dateToString);

    public static final GraphQLScalarType GraphQLSqlDate = temporalScalar(java.sql.Date.class,"SqlDate", "a SQL compliant local date",
            s -> java.sql.Date.valueOf(LocalDate.parse(s)), i -> java.sql.Date.valueOf(i.atZone(ZoneOffset.UTC).toLocalDate()), d -> d.toLocalDate().toString());

    public static final GraphQLScalarType GraphQLSqlTime = temporalScalar(Time.class,"SqlTime", "a SQL compliant local time",
            s -> Time.valueOf(LocalTime.parse(s)), i -> Time.valueOf(i.atZone(ZoneOffset.UTC).toLocalTime()), t -> t.toLocalTime().toString());

    public static final GraphQLScalarType GraphQLSqlTimestamp = temporalScalar(Timestamp.class,"SqlTimestamp", "a SQL compliant local date-time",
            s -> Timestamp.from(Instant.parse(s)), Timestamp::from, t -> t.toInstant().toString());

    public static final GraphQLScalarType GraphQLCalendar = temporalScalar(Calendar.class,"Calendar", "a date-time with a time-zone",
            s -> GregorianCalendar.from(ZonedDateTime.parse(s)), i -> GregorianCalendar.from(i.atZone(ZoneOffset.UTC)), c -> c.toInstant().toString());

    public static final GraphQLScalarType GraphQLInstant = temporalScalar(Instant.class, "Instant", "an instant in time",
            Instant::parse, i -> i);

    public static final GraphQLScalarType GraphQLLocalDate = temporalScalar(LocalDate.class, "LocalDate", "a local date",
            LocalDate::parse, i -> i.atZone(ZoneOffset.UTC).toLocalDate());

    public static final GraphQLScalarType GraphQLLocalTime = temporalScalar(LocalTime.class, "LocalTime", "a local time",
            LocalTime::parse, i -> i.atZone(ZoneOffset.UTC).toLocalTime());

    public static final GraphQLScalarType GraphQLLocalDateTime = temporalScalar(LocalDateTime.class, "LocalDateTime", "a local date-time",
            LocalDateTime::parse, i -> i.atZone(ZoneOffset.UTC).toLocalDateTime());

    public static final GraphQLScalarType GraphQLZonedDateTime = temporalScalar(ZonedDateTime.class, "ZonedDateTime", "a date-time with a time-zone",
            ZonedDateTime::parse, i -> i.atZone(ZoneOffset.UTC));

    public static final GraphQLScalarType GraphQLOffsetTime = temporalScalar(OffsetTime.class, "OffsetTime", "a time with a UTC offset",
            OffsetTime::parse, i -> OffsetTime.ofInstant(i, ZoneOffset.UTC));

    public static final GraphQLScalarType GraphQLOffsetDateTime = temporalScalar(OffsetDateTime.class, "OffsetDateTime", "a date-time with a UTC offset",
            OffsetDateTime::parse, i -> i.atOffset(ZoneOffset.UTC));

    public static final GraphQLScalarType GraphQLDurationScalar = temporalScalar(Duration.class, "Duration", "an amount of time",
            Duration::parse, instant -> Duration.ofMillis(instant.toEpochMilli()));

    public static final GraphQLScalarType GraphQLPeriodScalar = temporalScalar(Period.class, "Period", "a period of time",
            Period::parse, instant -> { throw new GraphQLException("Period can not be deserialized from a numeric value"); });

    private static String dateToString(Date date) {
        if (Date.class.equals(date.getClass())) {
            return date.toInstant().toString();
        } else if (isScalar(date.getClass())) {
            return Scalars.toGraphQLScalarType(date.getClass()).getCoercing().serialize(date).toString();
        } else {
            throw serializationException(date, Date.class, java.sql.Date.class, Time.class, Timestamp.class);
        }
    }

    private static Coercing<Object, Object> MAP_SCALAR_COERCION = new Coercing<Object, Object>() {
        @Override
        public Object serialize(Object dataFetcherResult) {
            return dataFetcherResult;
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Map) {
                return input;
            }
            throw valueParsingException(input, Map.class);
        }

        @Override
        public Object parseLiteral(Object input) throws CoercingParseLiteralException {
            return parseLiteral(input, Collections.emptyMap());
        }

        @Override
        public Object parseLiteral(Object input, Map<String, Object> variables) {
            return parseObjectValue(literalOrException(input, ObjectValue.class), variables);
        }
    };

    public static GraphQLScalarType graphQLMapScalar(String name, GraphQLDirective[] directives) {
        return GraphQLScalarType.newScalar()
                .name(name)
                .description("Built-in scalar for map-like structures")
                .withDirectives(directives)
                .coercing(MAP_SCALAR_COERCION)
                .build();
    }

    private static Coercing<Object, Object> OBJECT_SCALAR_COERCION = new Coercing<Object, Object>() {
        @Override
        public Object serialize(Object dataFetcherResult) {
            GraphQLScalarType scalar = toGraphQLScalarType(dataFetcherResult.getClass());
            return scalar != null ? scalar.getCoercing().serialize(dataFetcherResult) : dataFetcherResult;
        }

        @Override
        public Object parseValue(Object input) {
            return input;
        }

        @Override
        public Object parseLiteral(Object input) throws CoercingParseLiteralException {
            return parseLiteral(input, Collections.emptyMap());
        }

        @Override
        public Object parseLiteral(Object input, Map<String, Object> variables) {
            return parseObjectValue(((Value) input), variables);
        }
    };

    public static GraphQLScalarType graphQLObjectScalar(String name, GraphQLDirective[] directives) {
        return GraphQLScalarType.newScalar()
                .name(name)
                .description("Built-in scalar for dynamic values")
                .withDirectives(directives)
                .coercing(OBJECT_SCALAR_COERCION)
                .build();
    }

    private static Object parseObjectValue(Value value, Map<String, Object> variables) {
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
            return ((EnumValue) value).getName();
        }
        if (value instanceof NullValue) {
            return null;
        }
        if (value instanceof ArrayValue) {
            return ((ArrayValue) value).getValues().stream()
                    .map(v -> parseObjectValue(v, variables))
                    .collect(Collectors.toList());
        }
        if (value instanceof VariableReference) {
            return variables.get(((VariableReference) value).getName());
        }
        if (value instanceof ObjectValue) {
            Map<String, Object> map = new LinkedHashMap<>();
            ((ObjectValue) value).getObjectFields().forEach(field ->
                    map.put(field.getName(), parseObjectValue(field.getValue(), variables)));
            return map;
        }
        //Should never happen
        throw new CoercingParseLiteralException("Unknown scalar AST type: " + value.getClass().getName());
    }

    public static <T> GraphQLScalarType temporalScalar(Class<T> type, String name, String description, ThrowingFunction<String, T> fromString, ThrowingFunction<Instant, T> fromDate) {
        return temporalScalar(type, name, description, fromString, fromDate, Object::toString);
    }

    public static <T> GraphQLScalarType temporalScalar(Class<T> type, String name, String description, ThrowingFunction<String, T> fromString, ThrowingFunction<Instant, T> fromDate, ThrowingFunction<T, String> toString) {
        return GraphQLScalarType.newScalar()
                .name(name)
                .description("Built-in scalar representing " + description)
                .coercing(new Coercing<T, String>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public String serialize(Object dataFetcherResult) {
                        if (type.isInstance(dataFetcherResult)) {
                            try {
                                return toString.apply((T) dataFetcherResult);
                            } catch (Exception e) {
                                throw new CoercingSerializeException("Value " + dataFetcherResult + " could not be serialized");
                            }
                        }
                        throw serializationException(dataFetcherResult, type);
                    }

                    @Override
                    public T parseValue(Object input) {
                        try {
                            if (input instanceof String) {
                                return fromString.apply((String) input);
                            }
                            if (input instanceof Long) {
                                return fromDate.apply(Instant.ofEpochMilli((Long) input));
                            }
                            if (type.isInstance(input)) {
                                return type.cast(input);
                            }
                            throw valueParsingException(input, String.class, Long.class);
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Value " + input + " could not be parsed into a " + name);
                        }
                    }

                    @Override
                    public T parseLiteral(Object input) {
                        try {
                            if (input instanceof StringValue) {
                                return fromString.apply(((StringValue) input).getValue());
                            } else if (input instanceof IntValue) {
                                return fromDate.apply(Instant.ofEpochMilli(((IntValue) input).getValue().longValue()));
                            } else {
                                throw literalParsingException(input, StringValue.class, IntValue.class);
                            }
                        } catch (Exception e) {
                            throw new CoercingParseLiteralException("Value " + input + " could not be parsed into a " + name);
                        }
                    }
                }).build();
    }

    public static <T extends Value> T literalOrException(Object input, Class<T> valueType) {
        if (valueType.isInstance(input)) {
            return valueType.cast(input);
        }
        throw new CoercingParseLiteralException(errorMessage(input, valueType));
    }

    public static CoercingParseLiteralException literalParsingException(Object input, Class... allowedTypes) {
        return new CoercingParseLiteralException(errorMessage(input, allowedTypes));
    }

    public static CoercingParseValueException valueParsingException(Object input, Class... allowedTypes) {
        return new CoercingParseValueException(errorMessage(input, allowedTypes));
    }

    public static CoercingSerializeException serializationException(Object input, Class... allowedTypes) {
        return new CoercingSerializeException(errorMessage(input, allowedTypes));
    }

    public static String errorMessage(Object input, Class... allowedTypes) {
        String types = Arrays.stream(allowedTypes)
                .map(type -> "'" + type.getSimpleName() + "'")
                .collect(Collectors.joining(" or "));
        return String.format("Expected %stype %s but was '%s'", input instanceof Value ? "AST " : "",
                types, input == null ? "null" : input.getClass().getSimpleName());
    }

    private static final Map<Type, GraphQLScalarType> SCALAR_MAPPING = getScalarMapping();

    public static boolean isScalar(Type javaType) {
        return SCALAR_MAPPING.containsKey(javaType)
                //Class<> needs to be dealt with separately because it's the only generic type
                //If more generic types are added, find a better solution
                || (javaType instanceof ParameterizedType && ((ParameterizedType) javaType).getRawType().equals(Class.class));
    }

    public static GraphQLScalarType toGraphQLScalarType(Type javaType) {
        return SCALAR_MAPPING.get(GenericTypeReflector.erase(javaType));
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
        scalarMapping.put(URL.class, GraphQLUrl);
        scalarMapping.put(byte[].class, GraphQLBase64String);
        scalarMapping.put(Class.class, GraphQLClass);
        scalarMapping.put(Date.class, GraphQLDate);
        scalarMapping.put(java.sql.Date.class, GraphQLSqlDate);
        scalarMapping.put(Time.class, GraphQLSqlTime);
        scalarMapping.put(Timestamp.class, GraphQLSqlTimestamp);
        scalarMapping.put(Calendar.class, GraphQLCalendar);
        scalarMapping.put(Instant.class, GraphQLInstant);
        scalarMapping.put(LocalDate.class, GraphQLLocalDate);
        scalarMapping.put(LocalTime.class, GraphQLLocalTime);
        scalarMapping.put(LocalDateTime.class, GraphQLLocalDateTime);
        scalarMapping.put(ZonedDateTime.class, GraphQLZonedDateTime);
        scalarMapping.put(OffsetTime.class, GraphQLOffsetTime);
        scalarMapping.put(OffsetDateTime.class, GraphQLOffsetDateTime);
        scalarMapping.put(Duration.class, GraphQLDurationScalar);
        scalarMapping.put(Period.class, GraphQLPeriodScalar);
        scalarMapping.put(Locale.class, GraphQLLocale);
        return Collections.unmodifiableMap(scalarMapping);
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }
}
