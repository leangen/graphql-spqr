package io.leangen.graphql.metadata;

import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;
import java.util.stream.Collectors;

public class InputField {

    private final String name;
    private final String description;
    private final TypedElement typedElement;
    private final AnnotatedType deserializableType;
    private final Object defaultValue;

    public InputField(String name, String description, TypedElement element, AnnotatedType deserializableType, Object defaultValue) {
        this.name = Utils.requireNonEmpty(name);
        this.description = description;
        this.typedElement = Objects.requireNonNull(element);
        this.deserializableType = deserializableType != null ? deserializableType : element.getJavaType();
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AnnotatedType getJavaType() {
        return typedElement.getJavaType();
    }

    public AnnotatedType getDeserializableType() {
        return deserializableType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public TypedElement getTypedElement() {
        return typedElement;
    }

    @Override
    public String toString() {
        return String.format("Input field '%s' of type %s bound to [%s]", name, ClassUtils.toString(getJavaType()),
                typedElement.getElements().stream().map(ClassUtils::toString).collect(Collectors.joining()));
    }
}
