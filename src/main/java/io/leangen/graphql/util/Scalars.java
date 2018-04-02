package io.leangen.graphql.util;

import graphql.GraphQLException;
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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            return input instanceof String ? UUID.fromString((String) input) : input;
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
            return input instanceof String ? URI.create((String) input) : input;
        }

        @Override
        public Object parseLiteral(Object input) {
            if (!(input instanceof StringValue)) return null;
            return URI.create(((StringValue) input).getValue());
        }
    });

    public static final GraphQLScalarType GraphQLLocale = new GraphQLScalarType("Locale", "Built-in Locale", new Coercing() {
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
                return input instanceof String ? Locale.forLanguageTag((String) input) : input;
        }

        @Override
        public Object parseLiteral(Object input) {
            if (!(input instanceof StringValue)) return null;
            return Locale.forLanguageTag(((StringValue) input).getValue());
        }
    });
    
    public static final GraphQLScalarType GraphQLDate = temporalScalar("Date", "an instant in time",
            s -> new Date(Instant.parse(s).toEpochMilli()), i -> new Date(i.toEpochMilli()), d -> d.toInstant().toString());

    public static final GraphQLScalarType GraphQLSqlDate = temporalScalar("SqlDate", "a SQL compliant local date",
            s -> java.sql.Date.valueOf(LocalDate.parse(s)), i -> java.sql.Date.valueOf(i.atZone(ZoneOffset.UTC).toLocalDate()), d -> d.toLocalDate().toString());

    public static final GraphQLScalarType GraphQLCalendar = temporalScalar("Calendar", "a date-time with a time-zone",
            s -> GregorianCalendar.from(ZonedDateTime.parse(s)), i -> GregorianCalendar.from(i.atZone(ZoneOffset.UTC)), c -> c.toInstant().toString());

    public static final GraphQLScalarType GraphQLInstant = temporalScalar("Instant", "an instant in time",
            Instant::parse, Function.identity());

    public static final GraphQLScalarType GraphQLLocalDate = temporalScalar("LocalDate", "a local date",
            LocalDate::parse, i -> i.atZone(ZoneOffset.UTC).toLocalDate());
       
    public static final GraphQLScalarType GraphQLLocalTime = temporalScalar("LocalTime", "a local time",
            LocalTime::parse, i -> i.atZone(ZoneOffset.UTC).toLocalTime());
    
    public static final GraphQLScalarType GraphQLLocalDateTime = temporalScalar("LocalDateTime", "a local date-time",
            LocalDateTime::parse, i -> i.atZone(ZoneOffset.UTC).toLocalDateTime());

    public static final GraphQLScalarType GraphQLZonedDateTime = temporalScalar("ZonedDateTime", "a date-time with a time-zone",
            ZonedDateTime::parse, i -> i.atZone(ZoneOffset.UTC));

    public static final GraphQLScalarType GraphQLOffsetDateTime = temporalScalar("OffsetDateTime", "a date-time with a UTC offset",
            OffsetDateTime::parse, i -> i.atOffset(ZoneOffset.UTC));

    public static final GraphQLScalarType GraphQLDurationScalar = temporalScalar("Duration", "an amount of time",
            Duration::parse, instant -> Duration.ofMillis(instant.toEpochMilli()));
    
    public static final GraphQLScalarType GraphQLPeriodScalar = temporalScalar("Period", "a period of time",
            Period::parse, instant -> { throw new GraphQLException("Period can not be deserialized from a numeric value"); });
    
    public static GraphQLScalarType graphQLObjectScalar(String name) {
        return new GraphQLScalarType(name, "Built-in scalar for dynamic structures", new Coercing() {

            @Override
            public Object serialize(Object input) {
                return input;
            }

            @Override
            public Object parseValue(Object input) {
                return input;
            }

            @Override
            public Object parseLiteral(Object input) {
                if (!(input instanceof Value)) return null;
                return parseFieldValue(((Value) input));
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
                    return ((EnumValue) value).getName();
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

    public static <T> GraphQLScalarType temporalScalar(String name, String description, Function<String, T> fromString, Function<Instant, T> fromDate) {
        return temporalScalar(name, description, fromString, fromDate, Object::toString);
    }

    public static <T> GraphQLScalarType temporalScalar(String name, String description, Function<String, T> fromString, Function<Instant, T> fromDate, Function<T, String> toString) {
        return new GraphQLScalarType(name, "Built-in scalar representing " + description, new Coercing() {

            @Override
            @SuppressWarnings("unchecked")
            public String serialize(Object input) {
                if (input == null) return null;
                return toString.apply((T) input);
            }

            @Override
            public Object parseValue(Object input) {
                if (input instanceof String) {
                    return fromString.apply((String) input);
                }
                if (input instanceof Long) {
                    return fromDate.apply(Instant.ofEpochMilli((Long) input));
                }
                return input;
            }

            @Override
            public T parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    return fromString.apply(((StringValue) input).getValue());
                } else if (input instanceof IntValue) {
                    return fromDate.apply(Instant.ofEpochMilli(((IntValue) input).getValue().longValue()));
                } else {
                    return null;
                }
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
        scalarMapping.put(Date.class, GraphQLDate);
        scalarMapping.put(java.sql.Date.class, GraphQLSqlDate);
        scalarMapping.put(Calendar.class, GraphQLCalendar);
        scalarMapping.put(Instant.class, GraphQLInstant);
        scalarMapping.put(LocalDate.class, GraphQLLocalDate);
        scalarMapping.put(LocalTime.class, GraphQLLocalTime);
        scalarMapping.put(LocalDateTime.class, GraphQLLocalDateTime);
        scalarMapping.put(ZonedDateTime.class, GraphQLZonedDateTime);
        scalarMapping.put(OffsetDateTime.class, GraphQLOffsetDateTime);
        scalarMapping.put(Duration.class, GraphQLDurationScalar);
        scalarMapping.put(Period.class, GraphQLPeriodScalar);
        scalarMapping.put(Locale.class, GraphQLLocale);
        return Collections.unmodifiableMap(scalarMapping);
    }
}
