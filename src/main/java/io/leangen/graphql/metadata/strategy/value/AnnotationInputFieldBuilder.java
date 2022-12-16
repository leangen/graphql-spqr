package io.leangen.graphql.metadata.strategy.value;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationInputFieldBuilder implements InputFieldBuilder {

    @Override
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        return ClassUtils.getAnnotationFields(ClassUtils.getRawType(params.getType().getType())).stream()
                .map(method -> new InputField(
                        AnnotationMappingUtils.inputFieldName(method),
                        AnnotationMappingUtils.inputFieldDescription(method),
                        new TypedElement(GenericTypeReflector.annotate(method.getReturnType()), method),
                        null,
                        AnnotationMappingUtils.inputFieldDefaultValue(method)
                ))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isAnnotation();
    }
}
