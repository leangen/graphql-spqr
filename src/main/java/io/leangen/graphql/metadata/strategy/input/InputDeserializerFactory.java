package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface InputDeserializerFactory {
    
    InputDeserializer getDeserializer(Set<Type> abstractTypes);
}
