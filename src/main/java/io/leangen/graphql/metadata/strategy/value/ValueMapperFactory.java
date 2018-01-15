package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import io.leangen.graphql.execution.GlobalEnvironment;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ValueMapperFactory<T extends ValueMapper> {
    
    default T getValueMapper() {
        return getValueMapper(Collections.emptySet(), null);
    }
    
    T getValueMapper(Set<Type> abstractTypes, GlobalEnvironment environment);
}
