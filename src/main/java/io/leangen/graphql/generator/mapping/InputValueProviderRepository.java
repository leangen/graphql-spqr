package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InputValueProviderRepository {
    
    private final List<InputValueProvider> inputValueProviders = new ArrayList<>();

    public void registerProviders(InputValueProvider... inputValueProviders) {
        addAll(this.inputValueProviders, inputValueProviders);
    }
    
    public InputValueProvider getInputProvider(AnnotatedType inputType) {
        return inputValueProviders.stream().filter(conv -> conv.supports(inputType)).findFirst().orElse(null);
    }
    
    public boolean isEmpty() {
        return inputValueProviders.isEmpty();
    }
}
