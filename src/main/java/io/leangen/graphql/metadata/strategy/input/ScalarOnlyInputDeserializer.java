package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public class ScalarOnlyInputDeserializer implements InputDeserializer {

    @Override
    public <T> T deserialize(Object graphQlInput, Type type) {
        if (graphQlInput.getClass() == type) {
            return (T) graphQlInput;
        }
        throw new IllegalArgumentException("Deserialization failed");
    }
}
