package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Base64;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

public class ByteArrayToBase64Adapter extends AbstractTypeAdapter<byte[], String> {

    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public byte[] convertInput(String substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return decoder.decode(substitute);
    }

    @Override
    public String convertOutput(byte[] original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return encoder.encodeToString(original);
    }
}
