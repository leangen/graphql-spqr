package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public interface InputDeserializer {

    <T> T deserialize(Object graphQlInput, AnnotatedType type);
}
