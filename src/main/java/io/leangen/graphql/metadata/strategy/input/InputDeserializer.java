package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.AnnotatedType;

public interface InputDeserializer {

    <T> T deserialize(Object graphQlInput, AnnotatedType type);

    <T> T deserializeString(String json, AnnotatedType type);
}
