package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;

import static java.util.Collections.addAll;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ConverterRepository {

    private final List<InputConverter> inputConverters = new ArrayList<>();
    private final List<OutputConverter> outputConverters = new ArrayList<>();

    public void registerConverters(InputConverter... inputConverters) {
        addAll(this.inputConverters, inputConverters);
    }

    public void registerConverters(OutputConverter... outputConverters) {
        addAll(this.outputConverters, outputConverters);
    }

    public InputConverter getInputConverter(AnnotatedType inputType) {
        return inputConverters.stream().filter(conv -> conv.supports(inputType)).findFirst().orElse(null);
    }

    public OutputConverter getOutputConverter(AnnotatedType outputType) {
        return outputConverters.stream().filter(conv -> conv.supports(outputType)).findFirst().orElse(null);
    }

    public boolean isEmpty() {
        return inputConverters.isEmpty() && outputConverters.isEmpty();
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
