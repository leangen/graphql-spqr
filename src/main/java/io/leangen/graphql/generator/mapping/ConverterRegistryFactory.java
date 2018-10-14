package io.leangen.graphql.generator.mapping;

import java.util.List;

public interface ConverterRegistryFactory {
    ConverterRegistry create(List<InputConverter> inputConverters, List<OutputConverter> outputConverters);
}
