package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public class ScalarOnlyInputDeserializer implements InputDeserializer {

    private static final String DESERIALIZATION_ERROR = "Simple deserialization failed. " +
            "For complex object support, add Gson or Jackson to classpath, or implement a custom InputDeserializerFactory.";
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Object graphQlInput, Type sourceType, AnnotatedType type) {
        if (graphQlInput.getClass() == type.getType()) {
            return (T) graphQlInput;
        }
        throw new IllegalArgumentException(DESERIALIZATION_ERROR);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserializeString(String json, AnnotatedType type) {
        if (type.getType() == String.class) {
            return (T) json;
        }
        throw new IllegalArgumentException(DESERIALIZATION_ERROR);
    }
}
