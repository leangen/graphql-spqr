package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.util.Defaults;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    private final ValueMapper valueMapper;

    public JsonDefaultValueProvider(GlobalEnvironment environment) {
        this.valueMapper =  Defaults.valueMapperFactory(new DefaultTypeInfoGenerator())
                .getValueMapper(Collections.emptyMap(), environment);
    }

    @Override
    public DefaultValue getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, DefaultValue initialValue) {
        return initialValue.map(v -> valueMapper.fromString((String) v, type));
    }
}
