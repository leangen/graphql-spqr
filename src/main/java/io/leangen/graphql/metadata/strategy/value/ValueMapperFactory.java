package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ValueMapperFactory {
    
    default ValueMapper getValueMapper() {
        return getValueMapper(Collections.emptySet());
    }
    
    ValueMapper getValueMapper(Set<Type> abstractTypes);
}
