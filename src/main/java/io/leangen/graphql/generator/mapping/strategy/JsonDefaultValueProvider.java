package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.metadata.QueryArgument;
import io.leangen.graphql.metadata.QueryArgumentDefaultValue;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JsonDefaultValueProvider implements DefaultValueProvider {

    public final QueryArgumentDefaultValue value;

    public JsonDefaultValueProvider(QueryArgumentDefaultValue value) {
        this.value = value;
    }

    @Override
    public QueryArgumentDefaultValue getDefaultValue(QueryArgument argument, InputDeserializer inputDeserializer, BuildContext buildContext) {
        if (value.isEmpty()) {
            return value;
        } else {
            AnnotatedType mappableType = buildContext.executionContext.getMappableType(argument.getJavaType());
            return new QueryArgumentDefaultValue(inputDeserializer.deserializeString(
                    value.get(), mappableType));
        }
    }
}
