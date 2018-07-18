package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Objects;

public class OperationArgument {

    private final AnnotatedType javaType;
    private final String name;
    private final String description;
    private final Object defaultValue;
    private final Parameter parameter;
    private final boolean context;
    private final boolean mappable;

    public OperationArgument(AnnotatedType javaType, String name, String description, Object defaultValue,
                             Parameter parameter, boolean context, boolean mappable) {
        
        this.javaType = Objects.requireNonNull(javaType);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameter = parameter;
        this.context = context;
        this.mappable = mappable;
    }

    public AnnotatedType getJavaType() {
        return javaType;
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
        return parameter;
    }

    public boolean isContext() {
        return context;
    }

    public boolean isMappable() {
        return mappable;
    }
}
