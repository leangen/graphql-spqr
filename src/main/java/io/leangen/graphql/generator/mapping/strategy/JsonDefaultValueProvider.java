package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.Defaults;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    private final ValueMapper valueMapper;

    public JsonDefaultValueProvider() {
        this.valueMapper =  Defaults.valueMapperFactory(new DefaultTypeInfoGenerator())
                .getValueMapper();
    }

    @Override
    public Object getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, Object initialValue) {
        if (initialValue == null || (type.getType().equals(String.class) && initialValue instanceof String && !((String) initialValue).startsWith("\""))) {
            return initialValue;
        } else {
            return valueMapper.fromString((String) initialValue, type);
        }
    }
}
