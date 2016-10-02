package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;

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
}
