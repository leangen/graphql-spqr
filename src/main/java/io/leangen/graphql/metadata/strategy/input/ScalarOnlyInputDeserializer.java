package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public class ScalarOnlyInputDeserializer implements InputDeserializer {

    @Override
    public <T> T deserialize(Object graphQlInput, AnnotatedType type) {
        if (graphQlInput.getClass() == type.getType()) {
            return (T) graphQlInput;
        }
        throw new IllegalArgumentException("Deserialization failed");
    }
}
