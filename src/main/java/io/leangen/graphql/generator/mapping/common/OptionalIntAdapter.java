package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractSimpleTypeAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalInt;

public class OptionalIntAdapter extends AbstractSimpleTypeAdapter<OptionalInt, Integer> {
    
    @Override
    public Integer convertOutput(OptionalInt original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.isPresent() ? original.getAsInt() : null;
    }

    @Override
    public OptionalInt convertInput(Integer substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return substitute == null ? OptionalInt.empty() : OptionalInt.of(substitute);
    }
}
