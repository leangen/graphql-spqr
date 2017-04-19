package io.leangen.graphql.metadata.strategy.type;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Optional;

import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.annotations.types.GraphQLType;
import io.leangen.graphql.annotations.types.GraphQLUnion;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class DefaultTypeInfoGenerator implements TypeInfoGenerator {

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
    @SuppressWarnings("unchecked")
    public String generateTypeDescription(AnnotatedType type) {
        Optional<String>[] descriptions = new Optional[]{
                Optional.ofNullable(type.getAnnotation(GraphQLUnion.class))
                        .map(GraphQLUnion::description),
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::description),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::description)
        };
        return getFirstNonEmptyOrDefault(descriptions, "");
    }

    @SuppressWarnings("unchecked")
    private String generateSimpleName(AnnotatedType type) {
        Optional<String>[] names = new Optional[]{
                Optional.ofNullable(type.getAnnotation(GraphQLUnion.class))
                        .map(GraphQLUnion::name),
                Optional.ofNullable(type.getAnnotation(GraphQLInterface.class))
                        .map(GraphQLInterface::name),
                Optional.ofNullable(type.getAnnotation(GraphQLType.class))
                        .map(GraphQLType::name)
        };
        return getFirstNonEmptyOrDefault(names, ClassUtils.getRawType(type.getType()).getSimpleName());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private String getFirstNonEmptyOrDefault(Optional<String>[] optionals, String defaultValue) {
        return Arrays.stream(optionals)
                .map(opt -> opt.filter(Utils::notEmpty))
                .reduce(Utils::or)
                .map(opt -> opt.orElse(defaultValue))
                .get();
    }
}
