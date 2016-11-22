package io.leangen.graphql.query;

import java.lang.reflect.AnnotatedType;

import graphql.relay.Relay;
import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.proxy.ProxyFactory;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;

/**
 * Created by bojan.tomic on 5/14/16.
 */
public class ExecutionContext {

    public final Relay relay;
    public final TypeRepository typeRepository;
    public final ProxyFactory proxyFactory;
    public final IdTypeMapper idTypeMapper;
    public final InputDeserializer inputDeserializer;
    public final ConverterRepository converters;

    public ExecutionContext(Relay relay, TypeRepository typeRepository, ProxyFactory proxyFactory, IdTypeMapper idTypeMapper,
                            InputDeserializer inputDeserializer, ConverterRepository converters) {
        this.relay = relay;
        this.typeRepository = typeRepository;
        this.proxyFactory = proxyFactory;
        this.idTypeMapper = idTypeMapper;
        this.inputDeserializer = inputDeserializer;
        this.converters = converters;
    }

    @SuppressWarnings("unchecked")
    public Object convertOutput(Object output, AnnotatedType type) {
        OutputConverter outputConverter = this.converters.getOutputConverter(type);
        return outputConverter == null ? output : outputConverter.convertOutput(output, type, this);
    }

    @SuppressWarnings("unchecked")
    public Object convertInput(Object input, AnnotatedType type) {
        InputConverter inputConverter = this.converters.getInputConverter(type);
        return inputConverter == null ? input : inputConverter.convertInput(input, type, this);
    }
}
