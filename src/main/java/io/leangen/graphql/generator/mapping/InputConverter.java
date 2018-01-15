package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

/**
 * @param <T> The actual argument type of an exposed method
 * @param <S> The substitute type as which the argument values are to be deserialized
 */
public interface InputConverter<T, S> {

    T convertInput(S substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper);
    
    boolean supports(AnnotatedType type);

    /**
     * The returned type has to erase to {@code Class<S>}
     *
     * @param original The original type of the input value
     * @return The type of the input value this converter produces for the given original type
     */
    AnnotatedType getSubstituteType(AnnotatedType original);
}
