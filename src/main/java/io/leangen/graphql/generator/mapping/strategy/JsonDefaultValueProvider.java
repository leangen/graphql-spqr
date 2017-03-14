package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

import io.leangen.graphql.metadata.QueryArgumentDefaultValue;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeMetaDataGenerator;
import io.leangen.graphql.util.Defaults;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    @Override
    public QueryArgumentDefaultValue getDefaultValue(Parameter parameter, AnnotatedType parameterType, QueryArgumentDefaultValue initialValue) {
        if (initialValue.isEmpty()) {
            return initialValue;
        } else {
            return new QueryArgumentDefaultValue(
                    Defaults.valueMapperFactory(new DefaultTypeMetaDataGenerator())
                            .getValueMapper().fromString(initialValue.get(), parameterType));
        }
    }
}
