package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ConverterRegistry {

    private final List<InputConverter> inputConverters;
    private final List<OutputConverter> outputConverters;

    public ConverterRegistry(List<InputConverter> inputConverters, List<OutputConverter> outputConverters) {
        this.inputConverters = Collections.unmodifiableList(inputConverters);
        this.outputConverters = Collections.unmodifiableList(outputConverters);
    }

    public List<InputConverter> getInputConverters() {
        return inputConverters;
    }

    @SuppressWarnings("unchecked")
    public <T, S> InputConverter<T, S> getInputConverter(AnnotatedType inputType) {
        return (InputConverter<T, S>) inputConverters.stream().filter(conv -> conv.supports(inputType)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T, S> OutputConverter<T, S> getOutputConverter(AnnotatedType outputType) {
        return (OutputConverter<T, S>) outputConverters.stream().filter(conv -> conv.supports(outputType)).findFirst().orElse(null);
    }

    public ConverterRegistry forOperation(Operation operation) {
        List<AnnotatedType> returnTypes = operation.getResolvers().stream().map(Resolver::getReturnType).collect(Collectors.toList());
        List<OutputConverter> filteredByOperation = outputConverters.stream()
                .filter(conv -> conv.supports(new ConverterSupportParams(operation.getOperationType())))
                .collect(Collectors.toList());
        if (filteredByOperation.stream().noneMatch(conv -> returnTypes.stream().anyMatch(conv::supports))) {
            filteredByOperation = Collections.emptyList();
        }
        return new ConverterRegistry(inputConverters, filteredByOperation);
    }

    public AnnotatedType getMappableInputType(AnnotatedType type) {
        InputConverter converter = this.getInputConverter(type);
        if (converter != null) {
            return getMappableInputType(converter.getSubstituteType(type));
        }
        return ClassUtils.transformType(type, this::getMappableInputType);
    }
}
