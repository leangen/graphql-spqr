package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ConverterRepository {

    private final List<InputConverter> inputConverters;
    private final List<OutputConverter> outputConverters;

    public ConverterRepository(List<InputConverter> inputConverters, List<OutputConverter> outputConverters) {
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

    public AnnotatedType getMappableType(AnnotatedType type) {
        InputConverter converter = this.getInputConverter(type);
        if (converter != null) {
            return getMappableType(converter.getSubstituteType(type));
        }
        if (type.getType() instanceof Class) {
            return type;
        }
        if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) type;
            AnnotatedType[] arguments = Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .map(this::getMappableType)
                    .toArray(AnnotatedType[]::new);
            return TypeFactory.parameterizedAnnotatedClass(GenericTypeReflector.erase(type.getType()), type.getAnnotations(), arguments);
        }
        throw new IllegalArgumentException("Can not deserialize type: " + type.getType().getTypeName());
    }
}
