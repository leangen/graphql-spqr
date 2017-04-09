package io.leangen.graphql.metadata;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

import io.leangen.geantyref.GenericTypeReflector;

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

    @Override
    public int hashCode() {
        return Objects.hash(name, description) ^ GenericTypeReflector.hashCode(javaType);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof InputField)) {
            return false;
        }
        InputField that = (InputField) other;
        return Objects.equals(this.name, that.name) && Objects.equals(this.description, that.description)
                && GenericTypeReflector.equals(this.javaType, that.javaType);
    }
}
