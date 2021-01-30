package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.metadata.DefaultValue;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class IdentityDefaultValueProvider implements DefaultValueProvider {

    @Override
    public DefaultValue getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, DefaultValue initialValue) {
        return initialValue;
    }
}
