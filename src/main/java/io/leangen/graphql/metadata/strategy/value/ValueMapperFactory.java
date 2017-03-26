package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ValueMapperFactory<T extends ValueMapper> {
    
    default T getValueMapper() {
        return getValueMapper(Collections.emptySet());
    }
    
    T getValueMapper(Set<Type> abstractTypes);
}
