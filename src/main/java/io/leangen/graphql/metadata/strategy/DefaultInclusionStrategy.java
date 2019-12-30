package io.leangen.graphql.metadata.strategy;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public class DefaultInclusionStrategy implements InclusionStrategy {

    private final String[] basePackages;

    public DefaultInclusionStrategy(String... basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public boolean includeOperation(List<AnnotatedElement> elements, AnnotatedType declaringType) {
        return elements.stream().allMatch(element -> ClassUtils.isReal(element) && !isIgnored(element));
    }

    @Override
    public boolean includeArgument(Parameter parameter, AnnotatedType declaringType) {
        return ClassUtils.isReal(parameter) && !isDirectlyIgnored(parameter);
    }

    @Override
    public boolean includeArgumentForMapping(Parameter parameter, AnnotatedType parameterType, AnnotatedType declaringType) {
        return !isIgnored(parameter);
    }

    @Override
    public boolean includeInputField(InputFieldInclusionParams params) {
        return params.getElements().stream().noneMatch(this::isIgnored)
                && (params.isDirectlyDeserializable() || params.isDeserializableInSubType()) //is ever deserializable
                && isPackageAcceptable(params.getDeclaringType(), params.getElementDeclaringClass());
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean isPackageAcceptable(AnnotatedType type, Class<?> elementDeclaringClass) {
        Class<?> rawType = ClassUtils.getRawType(type.getType());
        String[] packages = new String[0];
        if (Utils.isArrayNotEmpty(this.basePackages)) {
            packages = this.basePackages;
        } else if (rawType.getPackage() != null) {
            packages = new String[] {rawType.getPackage().getName()};
        }
        packages = Arrays.stream(packages).filter(Utils::isNotEmpty).toArray(String[]::new); //remove the default package
        return elementDeclaringClass.equals(rawType)
                || Arrays.stream(packages).anyMatch(basePackage -> ClassUtils.isSubPackage(elementDeclaringClass.getPackage(), basePackage));
    }

    protected boolean isDirectlyIgnored(AnnotatedElement element) {
        return element.isAnnotationPresent(GraphQLIgnore.class);
    }

    protected boolean isIgnored(AnnotatedElement element) {
        return ClassUtils.hasAnnotation(element, GraphQLIgnore.class);
    }
}
