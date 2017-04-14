package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ArgumentInjectorRepository {
    
    private final List<ArgumentInjector> argumentInjectors;

    public ArgumentInjectorRepository(List<ArgumentInjector> argumentInjectors) {
        this.argumentInjectors = Collections.unmodifiableList(argumentInjectors);
    }

    public ArgumentInjector getInjector(AnnotatedType inputType) {
        return argumentInjectors.stream().filter(injector -> injector.supports(inputType)).findFirst().orElse(null);
    }
}
