package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.metadata.InputField;

import java.util.Set;

public interface InputFieldDiscoveryStrategy {
    
    Set<InputField> getInputFields(InputFieldDiscoveryParams params);
}
