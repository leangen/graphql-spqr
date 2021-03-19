package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypeDiscriminatorField;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public interface InputFieldBuilder {
    
    Set<InputField> getInputFields(InputFieldBuilderParams params);

    boolean supports(AnnotatedType type);

    default TypeDiscriminatorField getTypeDiscriminatorField(InputFieldBuilderParams params) {
        return null;
    }
}
