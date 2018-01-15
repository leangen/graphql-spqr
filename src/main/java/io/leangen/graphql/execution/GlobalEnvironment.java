package io.leangen.graphql.execution;

import java.lang.reflect.AnnotatedType;
import java.util.List;

import graphql.relay.Relay;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

@SuppressWarnings("WeakerAccess")
public class GlobalEnvironment {

    public final Relay relay;
    public final TypeRepository typeRepository;
    public final ConverterRepository converters;
    public final ArgumentInjectorRepository injectors;

    /**
     *
     * @param relay Relay mapping helper
     * @param typeRepository The repository of mapped types
     * @param converters Repository of all registered {@link io.leangen.graphql.generator.mapping.InputConverter}s
     *                   and {@link io.leangen.graphql.generator.mapping.OutputConverter}s
     * @param injectors The repository of registered argument injectors
     */
    public GlobalEnvironment(Relay relay, TypeRepository typeRepository, ConverterRepository converters, ArgumentInjectorRepository injectors) {
        this.relay = relay;
        this.typeRepository = typeRepository;
        this.converters = converters;
        this.injectors = injectors;
    }

    @SuppressWarnings("unchecked")
    public <T, S> T convertInput(S input, AnnotatedType type, ValueMapper valueMapper) {
        if (input == null) {
            return null;
        }
        InputConverter<T, S> inputConverter = this.converters.getInputConverter(type);
        return inputConverter == null ? (T) input : inputConverter.convertInput(input, type, this, valueMapper);
    }

    public AnnotatedType getMappableType(AnnotatedType type) {
        return this.converters.getMappableType(type);
    }

    public List<InputConverter> getInputConverters() {
        return this.converters.getInputConverters();
    }
}
