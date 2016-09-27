package io.leangen.graphql.query.conversion;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.addAll;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ConverterRepository {

    private final List<InputConverter> inputConverters = new ArrayList<>();
    private final List<OutputConverter> outputConverters = new ArrayList<>();
    private final Map<String, InputConverter> inputConvertersByPath = new HashMap<>();
    private final Map<String, OutputConverter> outputConvertersByPath = new HashMap<>();

    public void registerConverters(InputConverter... inputConverters) {
        addAll(this.inputConverters, inputConverters);
    }

    public void registerConverters(OutputConverter... outputConverters) {
        addAll(this.outputConverters, outputConverters);
    }

    public void registerInputConverterForPath(String path, AnnotatedType inputType) {
        InputConverter conv = getInputConverter(inputType);
        if (conv != null) {
            inputConvertersByPath.put(path, conv);
        }
    }

    public void registerOutputConverterForPath(String path, AnnotatedType outputType) {
        OutputConverter conv = getOutputConverter(outputType);
        if (conv != null) {
            outputConvertersByPath.put(path, conv);
        }
    }

    private InputConverter getInputConverter(AnnotatedType inputType) {
        return inputConverters.stream().filter(conv -> conv.supports(inputType)).findFirst().orElse(null);
    }

    private OutputConverter getOutputConverter(AnnotatedType outputType) {
        return outputConverters.stream().filter(conv -> conv.supports(outputType)).findFirst().orElse(null);
    }

    public InputConverter getInputConverterByPath(String path) {
        return inputConvertersByPath.get(path);
    }

    public OutputConverter getOutputConverterByPath(String path) {
        return outputConvertersByPath.get(path);
    }
}
