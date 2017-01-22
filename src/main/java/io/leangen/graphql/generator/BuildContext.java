package io.leangen.graphql.generator;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import graphql.relay.Relay;
import graphql.schema.TypeResolver;
import io.leangen.graphql.generator.mapping.ConverterRepository;
import io.leangen.graphql.generator.mapping.InputValueProviderRepository;
import io.leangen.graphql.generator.mapping.TypeMapperRepository;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeMetaDataGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.query.GlobalContext;

public class BuildContext {

    public final GlobalContext globalContext;
    public final QueryRepository queryRepository;
    public final TypeRepository typeRepository;
    public final TypeMapperRepository typeMappers;
    public final Relay relay;
    public final TypeResolver typeResolver;
    public final InterfaceMappingStrategy interfaceStrategy;
    public final ValueMapperFactory valueMapperFactory;
    public final TypeMetaDataGenerator typeMetaDataGenerator = new DefaultTypeMetaDataGenerator();

    public final Set<String> knownTypes = new HashSet<>();
    public final Set<String> knownInputTypes = new HashSet<>();
    public final Map<Type, Set<Type>> abstractComponentTypes = new HashMap<>();

    /**
     *
     * @param queryRepository Repository that can be used to fetch all known (singleton and domain) queries
     * @param typeMappers Repository of all registered {@link io.leangen.graphql.generator.mapping.TypeMapper}s
     * @param converters Repository of all registered {@link io.leangen.graphql.generator.mapping.InputConverter}s
     *                   and {@link io.leangen.graphql.generator.mapping.OutputConverter}s
     */
    public BuildContext(QueryRepository queryRepository, TypeMapperRepository typeMappers, ConverterRepository converters, 
                        InputValueProviderRepository inputProviders, InterfaceMappingStrategy interfaceStrategy, ValueMapperFactory valueMapperFactory) {
        this.queryRepository = queryRepository;
        this.typeRepository = new TypeRepository();
        this.typeMappers = typeMappers;
        this.relay = new Relay();
        this.typeResolver = new HintedTypeResolver(this.typeRepository, this.typeMetaDataGenerator);
        this.interfaceStrategy = interfaceStrategy;
        this.valueMapperFactory = valueMapperFactory;
        this.globalContext = new GlobalContext(relay, typeRepository, converters, inputProviders);
    }
}
