package io.leangen.graphql.module.common.jackson;

import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.module.SimpleModule;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.POJONode;

import java.util.Arrays;
import java.util.List;

public class JacksonModule implements SimpleModule {

    private static final JsonNodeAdapter jsonNodeAdapter = new JsonNodeAdapter();
    private static final JsonArrayAdapter jsonArrayAdapter = new JsonArrayAdapter();

    @Override
    public List<TypeMapper> getTypeMappers() {
        return Arrays.asList(jsonArrayAdapter, jsonNodeAdapter, new JacksonObjectScalarMapper());
    }

    @Override
    public List<OutputConverter<?, ?>> getOutputConverters() {
        return Arrays.asList(jsonArrayAdapter, jsonNodeAdapter);
    }

    @Override
    public List<InputConverter<?, ?>> getInputConverters() {
        return Arrays.asList(jsonArrayAdapter, jsonNodeAdapter);
    }

    @Override
    public void setUp(SetupContext context) {
        if (!getTypeMappers().isEmpty()) {
            context.withTypeMappers(getTypeMappers().toArray(new TypeMapper[0]));
        }
        if (!getOutputConverters().isEmpty()) {
            context.withOutputConverters(getOutputConverters().toArray(new OutputConverter[0]));
        }
        if (!getInputConverters().isEmpty()) {
            context.withInputConverters(getInputConverters().toArray(new InputConverter[0]));
        }
        context.withTypeComparator(ObjectNode.class, POJONode.class);
        context.withTypeComparator(DecimalNode.class, NumericNode.class);
    }
}
