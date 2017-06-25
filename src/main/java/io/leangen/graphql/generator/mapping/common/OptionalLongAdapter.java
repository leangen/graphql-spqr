package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.OptionalLong;

import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;

public class OptionalLongAdapter extends AbstractTypeAdapter<OptionalLong, Long> {
    
    @Override
    public Long convertOutput(OptionalLong original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.isPresent() ? original.getAsLong() : null;
    }

    @Override
    public OptionalLong convertInput(Long substitute, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return substitute == null ? OptionalLong.empty() : OptionalLong.of(substitute);
    }
}
