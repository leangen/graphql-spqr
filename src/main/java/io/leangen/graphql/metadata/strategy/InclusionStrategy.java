package io.leangen.graphql.metadata.strategy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.List;

public interface InclusionStrategy {

    boolean includeOperation(List<AnnotatedElement> elements, AnnotatedType declaringType);

    boolean includeArgument(Parameter parameter, AnnotatedType declaringType);

    boolean includeArgumentForMapping(Parameter parameter, AnnotatedType parameterType, AnnotatedType declaringType);

    boolean includeInputField(InputFieldInclusionParams params);
}
