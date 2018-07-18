package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public interface DefaultValueProvider {
    
    Object getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, Object initialValue);
}
