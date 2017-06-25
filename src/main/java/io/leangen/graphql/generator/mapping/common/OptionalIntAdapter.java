package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalInt;

import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;

public class OptionalIntAdapter extends AbstractTypeAdapter<OptionalInt, Integer> {
    
    @Override
    public Integer convertOutput(OptionalInt original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.isPresent() ? original.getAsInt() : null;
    }

    @Override
    public OptionalInt convertInput(Integer substitute, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return substitute == null ? OptionalInt.empty() : OptionalInt.of(substitute);
    }
}
