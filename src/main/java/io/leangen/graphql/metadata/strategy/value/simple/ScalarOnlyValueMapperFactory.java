package io.leangen.graphql.metadata.strategy.value.simple;

import java.lang.reflect.Type;
import java.util.Set;

import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ScalarOnlyValueMapperFactory implements ValueMapperFactory {
    
    private static final ValueMapper VALUE_MAPPER = new ScalarOnlyValueMapper();
    
    @Override
    public ValueMapper getValueMapper(Set<Type> abstractTypes) {
        return VALUE_MAPPER;
    }
}
