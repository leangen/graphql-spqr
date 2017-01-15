package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface InputDeserializerFactory {
    
    default InputDeserializer getDeserializer() {
        return getDeserializer(Collections.emptySet());
    }
    
    InputDeserializer getDeserializer(Set<Type> abstractTypes);
}
