package io.leangen.graphql.metadata.strategy.type;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;

import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class DefaultTypeMetaDataGenerator implements TypeMetaDataGenerator {

    @Override
    public String generateTypeName(AnnotatedType type) {
        if (type instanceof AnnotatedParameterizedType) {
            String baseName = generateSimpleName(type);
            StringBuilder genericName = new StringBuilder(baseName);
            Arrays.stream(((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments())
                    .map(this::generateSimpleName)
                    .forEach(argName -> genericName.append("_").append(argName));
            return genericName.toString();
        }
        return generateSimpleName(type);
    }

    @Override
    public String generateTypeDescription(AnnotatedType type) {
        if (type.isAnnotationPresent(GraphQLType.class)) {
            return type.getAnnotation(GraphQLType.class).description();
        } else {
            return ClassUtils.getRawType(type.getType()).getSimpleName();
        }
    }

    private String generateSimpleName(AnnotatedType type) {
        if (type.isAnnotationPresent(GraphQLType.class)) {
            return type.getAnnotation(GraphQLType.class).name();
        } else {
            return ClassUtils.getRawType(type.getType()).getSimpleName();
        }
    }
}
