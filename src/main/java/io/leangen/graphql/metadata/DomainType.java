package io.leangen.graphql.metadata;

import io.leangen.graphql.annotations.GraphQLType;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 3/2/16.
 */
public class DomainType {

    private String name;
    private String description;
    private AnnotatedType javaType;

    private static final String INPUT_SUFFIX = "_input";

    public DomainType(AnnotatedType javaType) {
        this.javaType = javaType;
        this.name = resolveName();
        this.description = resolveDescription();
    }

    private String resolveName() {
        if (javaType.isAnnotationPresent(GraphQLType.class)) {
            return javaType.getAnnotation(GraphQLType.class).name();
        } else {
            return ClassUtils.getRawType(javaType.getType()).getSimpleName();
        }
    }

    private String resolveDescription() {
        if (javaType.isAnnotationPresent(GraphQLType.class)) {
            return javaType.getAnnotation(GraphQLType.class).description();
        } else {
            return ClassUtils.getRawType(javaType.getType()).getSimpleName();
        }
    }

    public String getName() {
        return name;
    }

    public String getInputName() {
        return getName() + INPUT_SUFFIX;
    }

    public String getDescription() {
        return description;
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DomainType && this.javaType.equals(((DomainType) obj).javaType);
    }

    @Override
    public int hashCode() {
        return javaType.hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }
}
