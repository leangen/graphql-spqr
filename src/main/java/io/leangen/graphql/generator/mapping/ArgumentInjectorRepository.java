package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ArgumentInjectorRepository {
    
    private final List<ArgumentInjector> argumentInjectors = new ArrayList<>();

    public void registerInjectors(ArgumentInjector... argumentInjectors) {
        addAll(this.argumentInjectors, argumentInjectors);
    }
    
    public ArgumentInjector getInjector(AnnotatedType inputType) {
        return argumentInjectors.stream().filter(injector -> injector.supports(inputType)).findFirst().orElse(null);
    }
    
    public boolean isEmpty() {
        return argumentInjectors.isEmpty();
    }
}
