package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface ValueMapperFactory<T extends ValueMapper> {
    
    default T getValueMapper() {
        return getValueMapper(Collections.emptyMap(), GlobalEnvironment.EMPTY);
    }

    T getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment);
}
