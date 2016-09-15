package io.leangen.graphql.metadata.strategy.input;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

/**
 * Created by bojan.tomic on 6/6/16.
 */
public class JacksonInputDeserializer implements InputDeserializer {

    private ObjectMapper objectMapper;

    public JacksonInputDeserializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public <T> T deserialize(Object graphQlInput, Type type) {
        return objectMapper.convertValue(graphQlInput, objectMapper.getTypeFactory().constructType(type));
    }
}
