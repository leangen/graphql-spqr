package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ScalarOnlyInputDeserializerFactory implements InputDeserializerFactory {
    
    private static final InputDeserializer inputDeserializer = new ScalarOnlyInputDeserializer();
    
    @Override
    public InputDeserializer getDeserializer(Set<Type> abstractTypes) {
        return inputDeserializer;
    }
}
