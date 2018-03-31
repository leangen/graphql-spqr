package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public interface InputFieldDiscoveryStrategy {
    
    Set<InputField> getInputFields(AnnotatedType type, InclusionStrategy inclusionStrategy);
}
