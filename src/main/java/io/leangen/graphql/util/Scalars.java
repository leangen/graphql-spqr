package io.leangen.graphql.util;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * Created by bojan.tomic on 6/11/16.
 */
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
            if (input instanceof String || input instanceof URI) {
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
}
