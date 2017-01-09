package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public interface InputDeserializer {

    default <T> T deserialize(Object graphQlInput, AnnotatedType type) {
        return deserialize(graphQlInput, graphQlInput.getClass(), type);
    }
    
    <T> T deserialize(Object graphQlInput, Type sourceType, AnnotatedType outputType);

    <T> T deserializeString(String json, AnnotatedType type);
}
