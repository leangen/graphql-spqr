package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.metadata.InputField;

import java.util.Set;

public interface InputFieldBuilder {
    
    Set<InputField> getInputFields(InputFieldBuilderParams params);
}
