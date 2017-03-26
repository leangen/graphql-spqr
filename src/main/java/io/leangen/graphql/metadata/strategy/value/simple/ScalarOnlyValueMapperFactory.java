package io.leangen.graphql.metadata.strategy.value.simple;

import java.lang.reflect.Type;
import java.util.Set;

import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ScalarOnlyValueMapperFactory implements ValueMapperFactory<ScalarOnlyValueMapper> {
    
    private static final ScalarOnlyValueMapper VALUE_MAPPER = new ScalarOnlyValueMapper();
    
    @Override
    public ScalarOnlyValueMapper getValueMapper(Set<Type> abstractTypes) {
        return VALUE_MAPPER;
    }
}
