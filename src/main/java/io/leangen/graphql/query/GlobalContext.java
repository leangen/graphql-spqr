package io.leangen.graphql.query;

import graphql.relay.Relay;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.InputValueProviderRepository;

public class GlobalContext {

    public final Relay relay;
    public final TypeRepository typeRepository;
    public final ConverterRepository converters;
    public final InputValueProviderRepository providers;

    public GlobalContext(Relay relay, TypeRepository typeRepository, ConverterRepository converters, InputValueProviderRepository providers) {
        this.relay = relay;
        this.typeRepository = typeRepository;
        this.converters = converters;
        this.providers = providers;
    }
}
