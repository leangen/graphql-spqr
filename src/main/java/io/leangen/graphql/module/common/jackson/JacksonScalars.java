package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.leangen.graphql.util.Scalars.errorMessage;
import static io.leangen.graphql.util.Scalars.literalOrException;
import static io.leangen.graphql.util.Scalars.serializationException;
import static io.leangen.graphql.util.Scalars.valueParsingException;

@SuppressWarnings("WeakerAccess")
public class JacksonScalars {

    public static final GraphQLScalarType JsonTextNode = GraphQLScalarType.newScalar()
            .name("JsonText")
            .description("Text JSON node")
            .coercing(new Coercing<TextNode, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    } if (dataFetcherResult instanceof TextNode) {
                        return ((TextNode) dataFetcherResult).textValue();
                    } else {
                        throw serializationException(dataFetcherResult, String.class, TextNode.class);
                    }
                }

                @Override
                public TextNode parseValue(Object input) {
                    if (input instanceof String) {
                        return TextNode.valueOf((String) input);
                    }
                    if (input instanceof TextNode) {
                        return (TextNode) input;
                    }
                    throw valueParsingException(input, String.class, TextNode.class);
                }

                @Override
                public TextNode parseLiteral(Object input) {
                    return TextNode.valueOf(literalOrException(input, StringValue.class).getValue());
                }
            }).build();

    public static final GraphQLScalarType JsonBinaryNode = GraphQLScalarType.newScalar()
            .name("JsonBase64Binary")
            .description("Base64-encoded binary JSON node")
            .coercing(new Coercing<BinaryNode, String>() {
                private final Base64.Encoder encoder = Base64.getEncoder();
                private final Base64.Decoder decoder = Base64.getDecoder();

                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof BinaryNode) {
                        return encoder.encodeToString(((BinaryNode) dataFetcherResult).binaryValue());
                    }
                    if (dataFetcherResult instanceof String) {
                        return (String) dataFetcherResult;
                    }
                    throw serializationException(dataFetcherResult, String.class, BinaryNode.class);
                }

                @Override
                public BinaryNode parseValue(Object input) {
                    if (input instanceof String) {
                        return BinaryNode.valueOf(decoder.decode(input.toString()));
                    }
                    if (input instanceof BinaryNode) {
                        return (BinaryNode) input;
                    }
                    throw valueParsingException(input, String.class, BinaryNode.class);
                }

                @Override
                public BinaryNode parseLiteral(Object input) {
                    return new BinaryNode(decoder.decode(literalOrException(input, StringValue.class).getValue()));
                }
            }).build();

    public static final GraphQLScalarType JsonBooleanNode = GraphQLScalarType.newScalar()
            .name("JsonBoolean")
            .description("Boolean JSON node")
            .coercing(new Coercing<BooleanNode, Boolean>() {

                @Override
                public Boolean serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof BooleanNode) {
                        return ((BooleanNode) dataFetcherResult).booleanValue();
                    }
                    if (dataFetcherResult instanceof Boolean) {
                        return (Boolean) dataFetcherResult;
                    }
                    throw serializationException(dataFetcherResult, Boolean.class, BooleanNode.class);
                }

                @Override
                public BooleanNode parseValue(Object input) {
                    if (input instanceof Boolean) {
                        return (Boolean) input ? BooleanNode.TRUE : BooleanNode.FALSE;
                    }
                    if (input instanceof BooleanNode) {
                        return (BooleanNode) input;
                    }
                    throw valueParsingException(input, Boolean.class, BooleanNode.class);
                }

                @Override
                public BooleanNode parseLiteral(Object input) {
                    return literalOrException(input, BooleanValue.class).isValue() ? BooleanNode.TRUE : BooleanNode.FALSE;
                }
            }).build();

    public static final GraphQLScalarType JsonDecimalNode = GraphQLScalarType.newScalar()
            .name("JsonNumber")
            .description("Decimal JSON node")
            .coercing(new Coercing<DecimalNode, Number>() {

                @Override
                public Number serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof DecimalNode) {
                        return ((DecimalNode) dataFetcherResult).numberValue();
                    }
                    throw serializationException(dataFetcherResult, DecimalNode.class);
                }

                @Override
                public DecimalNode parseValue(Object input) {
                    if (input instanceof Number || input instanceof String) {
                        return (DecimalNode) JsonNodeFactory.instance.numberNode(new BigDecimal(input.toString()));
                    }
                    if (input instanceof DecimalNode) {
                        return (DecimalNode) input;
                    }
                    throw valueParsingException(input, Number.class, String.class, DecimalNode.class);
                }

                @Override
                public DecimalNode parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        return (DecimalNode) JsonNodeFactory.instance.numberNode(((IntValue) input).getValue());
                    } else if (input instanceof FloatValue) {
                        return (DecimalNode) JsonNodeFactory.instance.numberNode(((FloatValue) input).getValue());
                    } else {
                        throw new CoercingParseLiteralException(errorMessage(input, IntValue.class, FloatValue.class));
                    }
                }
            }).build();

    public static final GraphQLScalarType JsonIntegerNode = GraphQLScalarType.newScalar()
            .name("JsonInteger")
            .description("Integer JSON node")
            .coercing(new Coercing<IntNode, Number>() {

                @Override
                public Number serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof IntNode) {
                        return ((IntNode) dataFetcherResult).numberValue();
                    } else {
                        throw serializationException(dataFetcherResult, IntNode.class);
                    }
                }

                @Override
                public IntNode parseValue(Object input) {
                    if (input instanceof Number || input instanceof String) {
                        try {
                            return IntNode.valueOf(new BigInteger(input.toString()).intValueExact());
                        } catch (ArithmeticException e) {
                            throw new CoercingParseValueException(input + " does not fit into an int without a loss of precision");
                        }
                    }
                    if (input instanceof IntNode) {
                        return (IntNode) input;
                    }
                    throw valueParsingException(input, Number.class, String.class, IntNode.class);
                }

                @Override
                public IntNode parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        try {
                            return IntNode.valueOf(((IntValue) input).getValue().intValueExact());
                        } catch (ArithmeticException e) {
                            throw new CoercingParseLiteralException(input + " does not fit into an int without a loss of precision");
                        }
                    } else {
                        throw new CoercingParseLiteralException(errorMessage(input, IntValue.class));
                    }
                }
            }).build();

    public static final GraphQLScalarType JsonBigIntegerNode = GraphQLScalarType.newScalar()
            .name("JsonBigInteger")
            .description("BigInteger JSON node")
            .coercing(new Coercing<BigIntegerNode, Number>() {

                @Override
                public Number serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof BigIntegerNode) {
                        return ((BigIntegerNode) dataFetcherResult).numberValue();
                    } else {
                        throw serializationException(dataFetcherResult, BigIntegerNode.class);
                    }
                }

                @Override
                public BigIntegerNode parseValue(Object input) {
                    if (input instanceof Number || input instanceof String) {
                        return BigIntegerNode.valueOf(new BigInteger(input.toString()));
                    }
                    if (input instanceof BigIntegerNode) {
                        return (BigIntegerNode) input;
                    }
                    throw valueParsingException(input, Number.class, String.class, BigIntegerNode.class);
                }

                @Override
                public BigIntegerNode parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        return BigIntegerNode.valueOf(((IntValue) input).getValue());
                    } else {
                        throw new CoercingParseLiteralException(errorMessage(input, IntValue.class));
                    }
                }
            }).build();

    public static final GraphQLScalarType JsonShortNode = GraphQLScalarType.newScalar()
            .name("JsonShort")
            .description("Short JSON node")
            .coercing(new Coercing<ShortNode, Number>() {

                @Override
                public Number serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof ShortNode) {
                        return ((ShortNode) dataFetcherResult).numberValue();
                    } else {
                        throw serializationException(dataFetcherResult, ShortNode.class);
                    }
                }

                @Override
                public ShortNode parseValue(Object input) {
                    if (input instanceof Number || input instanceof String) {
                        try {
                            return ShortNode.valueOf(new BigInteger(input.toString()).shortValueExact());
                        } catch (ArithmeticException e) {
                            throw new CoercingParseValueException(input + " does not fit into a short without a loss of precision");
                        }
                    }
                    if (input instanceof ShortNode) {
                        return (ShortNode) input;
                    }
                    throw valueParsingException(input, Number.class, String.class, ShortNode.class);
                }

                @Override
                public ShortNode parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        try {
                            return ShortNode.valueOf(((IntValue) input).getValue().shortValueExact());
                        } catch (ArithmeticException e) {
                            throw new CoercingParseLiteralException(input + " does not fit into a short without a loss of precision");
                        }
                    } else {
                        throw new CoercingParseLiteralException(errorMessage(input, IntValue.class));
                    }
                }
            }).build();

    public static final GraphQLScalarType JsonFloatNode = GraphQLScalarType.newScalar()
            .name("JsonFloat")
            .description("Float JSON node")
            .coercing(new Coercing<FloatNode, Number>() {

                @Override
                public Number serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof FloatNode) {
                        return ((FloatNode) dataFetcherResult).numberValue();
                    } else {
                        throw serializationException(dataFetcherResult, FloatNode.class);
                    }
                }

                @Override
                public FloatNode parseValue(Object input) {
                    if (input instanceof Number || input instanceof String) {
                        return FloatNode.valueOf(new BigDecimal(input.toString()).floatValue());
                    }
                    if (input instanceof FloatNode) {
                        return (FloatNode) input;
                    }
                    throw valueParsingException(input, Number.class, String.class, FloatNode.class);
                }

                @Override
                public FloatNode parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        return FloatNode.valueOf(((IntValue) input).getValue().floatValue());
                    } if (input instanceof FloatValue) {
                        return FloatNode.valueOf(((FloatValue) input).getValue().floatValue());
                    } else {
                        throw new CoercingParseLiteralException(errorMessage(input, IntValue.class, FloatValue.class));
                    }
                }
            }).build();

    public static final GraphQLScalarType JsonDoubleNode = GraphQLScalarType.newScalar()
            .name("JsonDouble")
            .description("Double JSON node")
            .coercing(new Coercing<DoubleNode, Number>() {

                @Override
                public Number serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof DoubleNode) {
                        return ((DoubleNode) dataFetcherResult).numberValue();
                    } else {
                        throw serializationException(dataFetcherResult, DoubleNode.class);
                    }
                }

                @Override
                public DoubleNode parseValue(Object input) {
                    if (input instanceof Number || input instanceof String) {
                        return DoubleNode.valueOf(new BigDecimal(input.toString()).doubleValue());
                    }
                    if (input instanceof DoubleNode) {
                        return (DoubleNode) input;
                    }
                    throw valueParsingException(input, Number.class, String.class, DoubleNode.class);
                }

                @Override
                public DoubleNode parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        return DoubleNode.valueOf(((IntValue) input).getValue().doubleValue());
                    } if (input instanceof FloatValue) {
                        return DoubleNode.valueOf(((FloatValue) input).getValue().doubleValue());
                    } else {
                        throw new CoercingParseLiteralException(errorMessage(input, IntValue.class, FloatValue.class));
                    }
                }
            }).build();

    private static final Map<Type, GraphQLScalarType> SCALAR_MAPPING = getScalarMapping();

    public static boolean isScalar(Type javaType) {
        return SCALAR_MAPPING.containsKey(javaType);
    }

    public static GraphQLScalarType toGraphQLScalarType(Type javaType) {
        return SCALAR_MAPPING.get(javaType);
    }

    private static Map<Type, GraphQLScalarType> getScalarMapping() {
        Map<Type, GraphQLScalarType> scalarMapping = new HashMap<>();
        scalarMapping.put(TextNode.class, JsonTextNode);
        scalarMapping.put(BooleanNode.class, JsonBooleanNode);
        scalarMapping.put(BinaryNode.class, JsonBinaryNode);
        scalarMapping.put(BigIntegerNode.class, JsonBigIntegerNode);
        scalarMapping.put(IntNode.class, JsonIntegerNode);
        scalarMapping.put(ShortNode.class, JsonShortNode);
        scalarMapping.put(DecimalNode.class, JsonDecimalNode);
        scalarMapping.put(FloatNode.class, JsonFloatNode);
        scalarMapping.put(DoubleNode.class, JsonDoubleNode);
        scalarMapping.put(NumericNode.class, JsonDecimalNode);
        return Collections.unmodifiableMap(scalarMapping);
    }
}
