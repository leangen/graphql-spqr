package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonInputDeserializerFactory implements InputDeserializerFactory {
    
    @Override
    public InputDeserializer getDeserializer(Set<Type> abstractTypes) {
        return new JacksonInputDeserializer();
    }
}
