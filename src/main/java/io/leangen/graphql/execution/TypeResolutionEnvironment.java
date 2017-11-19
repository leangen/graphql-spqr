package io.leangen.graphql.execution;

import io.leangen.graphql.generator.TypeRepository;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;

public class TypeResolutionEnvironment extends graphql.TypeResolutionEnvironment {

    private final TypeRepository typeRepository;
    private final TypeInfoGenerator typeInfoGenerator;

    public TypeResolutionEnvironment(graphql.TypeResolutionEnvironment environment,
                                     TypeRepository typeRepository,
                                     TypeInfoGenerator typeInfoGenerator) {
        super(environment.getObject(), environment.getArguments(), environment.getField(), environment.getFieldType(), environment.getSchema(), environment.getContext());
        this.typeRepository = typeRepository;
        this.typeInfoGenerator = typeInfoGenerator;
    }

    public TypeRepository getTypeRepository() {
        return typeRepository;
    }

    public TypeInfoGenerator getTypeInfoGenerator() {
        return typeInfoGenerator;
    }
}
