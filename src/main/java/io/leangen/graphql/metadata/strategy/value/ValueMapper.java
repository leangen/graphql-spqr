package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public interface ValueMapper {

    default <T> T fromInput(Object graphQlInput, AnnotatedType type) {
        return fromInput(graphQlInput, graphQlInput.getClass(), type);
    }
    
    <T> T fromInput(Object graphQlInput, Type sourceType, AnnotatedType outputType);

    <T> T fromString(String json, AnnotatedType type);
    
    String toString(Object output);
}
