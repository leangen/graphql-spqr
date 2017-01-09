package io.leangen.graphql.generator;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

import graphql.relay.Relay;
import graphql.schema.TypeResolver;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.input.GsonInputDeserializerFactory;
import io.leangen.graphql.metadata.strategy.input.InputDeserializerFactory;
import io.leangen.graphql.query.DefaultIdTypeMapper;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.IdTypeMapper;
import io.leangen.graphql.util.collections.AnnotatedTypeSet;

public class BuildContext {

    public final ExecutionContext executionContext;
    public final QueryRepository queryRepository;
    public final TypeRepository typeRepository;
    public final IdTypeMapper idTypeMapper;
    public final TypeMapperRepository typeMappers;
    public final Relay relay;
    public final TypeResolver typeResolver;
    public final InterfaceMappingStrategy interfaceStrategy;
    public final InputDeserializerFactory inputDeserializerFactory;

    public final Set<AnnotatedType> knownTypes = new AnnotatedTypeSet();
    public final Set<AnnotatedType> knownInputTypes = new AnnotatedTypeSet();

    /**
     *
     * @param queryRepository Repository that can be used to fetch all known (singleton and domain) queries
     * @param typeMappers Repository of all registered {@link io.leangen.graphql.generator.mapping.TypeMapper}s
     * @param converters Repository of all registered {@link io.leangen.graphql.generator.mapping.InputConverter}s
     *                   and {@link io.leangen.graphql.generator.mapping.OutputConverter}s
     */
    public BuildContext(QueryRepository queryRepository, TypeMapperRepository typeMappers, ConverterRepository converters, InterfaceMappingStrategy interfaceStrategy) {
        this.queryRepository = queryRepository;
        this.typeRepository = new TypeRepository();
        this.idTypeMapper = new DefaultIdTypeMapper();
        this.typeMappers = typeMappers;
        this.relay = new Relay();
        this.typeResolver = new HintedTypeResolver(this.typeRepository, this.typeMappers);
        this.interfaceStrategy = interfaceStrategy;
        this.inputDeserializerFactory = new GsonInputDeserializerFactory();
        this.executionContext = new ExecutionContext(relay, typeRepository, idTypeMapper, converters);
    }
}
