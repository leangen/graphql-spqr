package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonValueMapperFactory implements ValueMapperFactory {
    
    @Override
    public ValueMapper getValueMapper(Set<Type> abstractTypes) {
        return new JacksonValueMapper();
    }
}
