package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

import io.leangen.graphql.metadata.InputField;

public interface InputFieldDiscoveryStrategy {
    
    Set<InputField> getInputFields(AnnotatedType type);
}
