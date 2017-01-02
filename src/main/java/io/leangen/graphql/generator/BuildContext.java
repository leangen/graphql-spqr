package io.leangen.graphql.generator;

import java.util.HashSet;
import java.util.Set;

import graphql.relay.Relay;
import graphql.schema.TypeResolver;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.strategy.AbstractTypeGenerationStrategy;
import io.leangen.graphql.generator.mapping.strategy.FlatTypeGenerationStrategy;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.query.DefaultIdTypeMapper;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.IdTypeMapper;

public class BuildContext {

    public final AbstractTypeGenerationStrategy typeStrategy;
    public final ExecutionContext executionContext;
    public final QueryRepository queryRepository;
    public final TypeRepository typeRepository;
    public final IdTypeMapper idTypeMapper;
    public final TypeMapperRepository typeMappers;
    public final Relay relay;
    public final TypeResolver typeResolver;
    public final InterfaceMappingStrategy interfaceStrategy;

    public final Set<String> inputsInProgress = new HashSet<>();

    /**
     *
     * @param queryRepository Repository that can be used to fetch all known (singleton and domain) queries
     * @param typeMappers Repository of all registered {@link io.leangen.graphql.generator.mapping.TypeMapper}s
     * @param converters Repository of all registered {@link io.leangen.graphql.generator.mapping.InputConverter}s
     *                   and {@link io.leangen.graphql.generator.mapping.OutputConverter}s
     */
    public BuildContext(QueryRepository queryRepository, TypeMapperRepository typeMappers, ConverterRepository converters, InterfaceMappingStrategy interfaceStrategy) {
        this.typeStrategy = new FlatTypeGenerationStrategy(queryRepository);
        this.queryRepository = queryRepository;
        this.typeRepository = new TypeRepository();
        this.idTypeMapper = new DefaultIdTypeMapper();
        this.typeMappers = typeMappers;
        this.relay = new Relay();
        this.typeResolver = new HintedTypeResolver(this.typeRepository, this.typeMappers);
        this.interfaceStrategy = interfaceStrategy;
        this.executionContext = new ExecutionContext(relay, typeRepository, idTypeMapper, converters);
    }
}
