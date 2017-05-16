package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

import io.leangen.graphql.metadata.OperationArgumentDefaultValue;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.Defaults;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    private final ValueMapper valueMapper;

    public JsonDefaultValueProvider() {
        this.valueMapper =  Defaults.valueMapperFactory(null, new DefaultTypeInfoGenerator())
                .getValueMapper();
    }

    @Override
    public OperationArgumentDefaultValue getDefaultValue(Parameter parameter, AnnotatedType parameterType, OperationArgumentDefaultValue initialValue) {
        if (initialValue.isEmpty()) {
            return initialValue;
        } else {
            return new OperationArgumentDefaultValue(valueMapper.fromString(initialValue.get(), parameterType));
        }
    }
}
