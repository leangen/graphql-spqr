package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    private final ValueMapper valueMapper;

    private static final Logger log = LoggerFactory.getLogger(JsonDefaultValueProvider.class);

    public JsonDefaultValueProvider() {
        this.valueMapper =  Defaults.valueMapperFactory(new DefaultTypeInfoGenerator())
                .getValueMapper();
    }

    @Override
    public Object getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, Object initialValue) {
        return valueMapper.fromString((String) initialValue, type);
    }
}
