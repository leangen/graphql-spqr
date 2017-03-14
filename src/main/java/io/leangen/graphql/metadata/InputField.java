package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;

public class InputField {

    private final String name;
    private final String description;
    private final AnnotatedType javaType;

    public InputField(String name, String description, AnnotatedType javaType) {
        this.name = name;
        this.description = description;
        this.javaType = javaType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }
}
