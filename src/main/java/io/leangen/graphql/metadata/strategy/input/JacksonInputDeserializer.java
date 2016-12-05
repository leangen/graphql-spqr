package io.leangen.graphql.metadata.strategy.input;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedType;

public class JacksonInputDeserializer implements InputDeserializer {

    private ObjectMapper objectMapper;

    public JacksonInputDeserializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public <T> T deserialize(Object graphQlInput, AnnotatedType type) {
        return objectMapper.convertValue(graphQlInput, objectMapper.getTypeFactory().constructType(type.getType()));
    }

    @Override
    public <T> T deserializeString(String json, AnnotatedType type) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type.getType()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
