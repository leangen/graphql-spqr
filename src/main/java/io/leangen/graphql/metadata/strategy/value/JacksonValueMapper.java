package io.leangen.graphql.metadata.strategy.value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public class JacksonValueMapper implements ValueMapper {

    private ObjectMapper objectMapper;

    public JacksonValueMapper() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public <T> T fromInput(Object graphQlInput, Type sourceType, AnnotatedType outputType) {
        return objectMapper.convertValue(graphQlInput, objectMapper.getTypeFactory().constructType(outputType.getType()));
    }

    @Override
    public <T> T fromString(String json, AnnotatedType type) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type.getType()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString(Object output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
