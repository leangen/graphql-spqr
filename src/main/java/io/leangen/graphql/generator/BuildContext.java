package io.leangen.graphql.generator;

import java.util.HashSet;
import java.util.Set;

import graphql.relay.Relay;
import graphql.schema.TypeResolver;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.proxy.ProxyFactory;
import io.leangen.graphql.generator.strategy.AbstractTypeGenerationStrategy;
import io.leangen.graphql.generator.strategy.FlatTypeGenerationStrategy;
import io.leangen.graphql.metadata.strategy.input.GsonInputDeserializer;
import io.leangen.graphql.query.DefaultIdTypeMapper;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.query.HintedTypeResolver;
import io.leangen.graphql.query.IdTypeMapper;

/**
 * Created by bojan.tomic on 3/30/16.
 */
public class BuildContext {

    public final AbstractTypeGenerationStrategy typeStrategy;
    public final ExecutionContext executionContext;
    public final QueryRepository queryRepository;
    public final TypeRepository typeRepository;
    public final ProxyFactory proxyFactory;
    public final IdTypeMapper idTypeMapper;
    public final TypeMapperRepository typeMappers;
    public final Relay relay;
    public final TypeResolver typeResolver;

    public final Set<String> inputsInProgress = new HashSet<>();

    /**
     *
     * @param queryRepository Repository that can be used to fetch all known (singleton and domain) queries
     * @param typeMappers Repository of all registered {@link io.leangen.graphql.generator.mapping.TypeMapper}s
     * @param converters Repository of all registered {@link io.leangen.graphql.generator.mapping.InputConverter}s
     *                   and {@link io.leangen.graphql.generator.mapping.OutputConverter}s
     */
    public BuildContext(QueryRepository queryRepository, TypeMapperRepository typeMappers, ConverterRepository converters) {
        this.typeStrategy = new FlatTypeGenerationStrategy(queryRepository);
        this.queryRepository = queryRepository;
        this.typeRepository = new TypeRepository();
        this.idTypeMapper = new DefaultIdTypeMapper();
        this.typeMappers = typeMappers;
        this.proxyFactory = new ProxyFactory();
        this.relay = new Relay();
        this.typeResolver = new HintedTypeResolver(this.typeRepository);
        this.executionContext = new ExecutionContext(relay, typeRepository, proxyFactory, idTypeMapper, new GsonInputDeserializer(), converters);
    }
}
