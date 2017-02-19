package io.leangen.graphql.metadata.strategy.value.jackson;

import java.lang.reflect.Type;
import java.util.Set;

import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonValueMapperFactory implements ValueMapperFactory {
    
    @Override
    public ValueMapper getValueMapper(Set<Type> abstractTypes) {
        return new JacksonValueMapper();
    }
}
