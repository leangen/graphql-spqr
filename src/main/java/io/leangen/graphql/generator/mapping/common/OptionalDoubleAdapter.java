package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractSimpleTypeAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalDouble;

public class OptionalDoubleAdapter extends AbstractSimpleTypeAdapter<OptionalDouble, Double> {
    
    @Override
    public Double convertOutput(OptionalDouble original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.isPresent() ? original.getAsDouble() : null;
    }

    @Override
    public OptionalDouble convertInput(Double substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return substitute == null ? OptionalDouble.empty() : OptionalDouble.of(substitute);
    }
}
