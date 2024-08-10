package io.leangen.graphql.metadata.strategy;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class DefaultInclusionStrategy implements InclusionStrategy {

    private final String[] basePackages;
    private final List<Predicate<AnnotatedElement>> operationElementFilters;

    public DefaultInclusionStrategy(String... basePackages) {
        this.basePackages = basePackages;
        this.operationElementFilters = new ArrayList<>();
    }

    @Override
    public boolean includeOperation(List<AnnotatedElement> elements, AnnotatedType declaringType) {
        return elements.stream().allMatch(element -> ClassUtils.isReal(element) && !isIgnored(element) && isAccepted(element));
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
    public boolean includeInputField(AnnotatedElement element) {
        return !isIgnored(element);
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
        if (Utils.isNotEmpty(this.basePackages)) {
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

    public DefaultInclusionStrategy excludeStaticMembers() {
        this.operationElementFilters.add(e -> !Modifier.isStatic(((Member) e).getModifiers()));
        return this;
    }

    private boolean isAccepted(AnnotatedElement element) {
        return operationElementFilters.stream().allMatch(filter -> filter.test(element));
    }
}
