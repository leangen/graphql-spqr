package io.leangen.graphql.metadata;

import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OperationArgument {

    private final TypedElement typedElement;
    private final String name;
    private final String description;
    private final Object defaultValue;
    private final boolean context;
    private final boolean mappable;

    public OperationArgument(AnnotatedType javaType, String name, String description, Object defaultValue,
                             Parameter parameter, boolean context, boolean mappable) {
        this(javaType, name, description, defaultValue, Utils.singletonList(parameter), context, mappable);
    }

    public OperationArgument(AnnotatedType javaType, String name, String description, Object defaultValue,
                             List<Parameter> parameters, boolean context, boolean mappable) {

        this.typedElement = new TypedElement(Objects.requireNonNull(javaType), parameters);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.defaultValue = defaultValue;
        this.context = context;
        this.mappable = mappable;
    }

    public AnnotatedType getJavaType() {
        return typedElement.getJavaType();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public Parameter getParameter() {
        return (Parameter) typedElement.getElement();
    }

    public boolean isContext() {
        return context;
    }

    public TypedElement getTypedElement() {
        return typedElement;
    }

    public boolean isMappable() {
        return mappable;
    }

    @Override
    public String toString() {
        return String.format("Argument '%s' of type %s bound to [%s]", name, ClassUtils.toString(getJavaType()),
                typedElement.getElements().stream().map(ClassUtils::toString).collect(Collectors.joining()));
    }
}
