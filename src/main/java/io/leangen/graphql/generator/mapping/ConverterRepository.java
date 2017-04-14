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

    public InputConverter getInputConverter(AnnotatedType inputType) {
        return inputConverters.stream().filter(conv -> conv.supports(inputType)).findFirst().orElse(null);
    }

    public OutputConverter getOutputConverter(AnnotatedType outputType) {
        return outputConverters.stream().filter(conv -> conv.supports(outputType)).findFirst().orElse(null);
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
