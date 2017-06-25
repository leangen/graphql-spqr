package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalDouble;

import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;

public class OptionalDoubleAdapter extends AbstractTypeAdapter<OptionalDouble, Double> {
    
    @Override
    public Double convertOutput(OptionalDouble original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.isPresent() ? original.getAsDouble() : null;
    }

    @Override
    public OptionalDouble convertInput(Double substitute, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return substitute == null ? OptionalDouble.empty() : OptionalDouble.of(substitute);
    }
}
