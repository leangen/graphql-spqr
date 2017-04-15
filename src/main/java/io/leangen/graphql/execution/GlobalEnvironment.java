package io.leangen.graphql.execution;

import graphql.relay.Relay;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.mapping.ArgumentInjectorRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;

@SuppressWarnings("WeakerAccess")
public class GlobalEnvironment {

    public final Relay relay;
    public final TypeRepository typeRepository;
    public final ConverterRepository converters;
    public final ArgumentInjectorRepository injectors;

    public GlobalEnvironment(Relay relay, TypeRepository typeRepository, ConverterRepository converters, ArgumentInjectorRepository injectors) {
        this.relay = relay;
        this.typeRepository = typeRepository;
        this.converters = converters;
        this.injectors = injectors;
    }
}
