package io.leangen.graphql.generator;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.TypeMapperRegistry;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Urls;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class Validator {

    private final GlobalEnvironment environment;
    private final TypeMapperRegistry mappers;
    private final Comparator<AnnotatedType> typeComparator;
    private final Map<String, AnnotatedType> mappedTypes;

    Validator(GlobalEnvironment env, TypeMapperRegistry mappers, Collection<GraphQLNamedType> knownTypes, Comparator<AnnotatedType> typeComparator) {
        this.environment = env;
        this.mappers = mappers;
        this.typeComparator = typeComparator;
        this.mappedTypes = knownTypes.stream()
                .filter(env.typeRegistry::isMappedType)
                .collect(Collectors.toMap(GraphQLNamedType::getName, type -> ClassUtils.normalize(env.typeRegistry.getMappedType(type))));
    }

    ValidationResult checkUniqueness(GraphQLOutputType graphQLType, AnnotatedElement element, AnnotatedType javaType) {
        return checkUniqueness(graphQLType, () -> mappers.getMappableType(element, javaType));
    }

    ValidationResult checkUniqueness(GraphQLInputType graphQLType, AnnotatedElement element, AnnotatedType javaType) {
        return checkUniqueness(graphQLType, () -> {
            AnnotatedType inputType = environment.getMappableInputType(javaType);
            if (GenericTypeReflector.equals(javaType, inputType)) {
                return mappers.getMappableType(element, javaType);
            }
            return inputType;
        });
    }

    private ValidationResult checkUniqueness(GraphQLType graphQLType, Supplier<AnnotatedType> javaType) {
        if (!(graphQLType instanceof GraphQLNamedType)) {
            return ValidationResult.valid();
        }
        GraphQLNamedType namedType = ((GraphQLNamedType) graphQLType);

        AnnotatedType resolvedType;
        try {
            resolvedType = resolveType(javaType);
        } catch (Exception e) {
            return ValidationResult.invalid(
                    String.format("Exception while checking the name uniqueness for %s: %s", namedType.getName(), e.getMessage()));
        }
        mappedTypes.putIfAbsent(namedType.getName(), resolvedType);
        AnnotatedType knownType = mappedTypes.get(namedType.getName());
        if (isMappingAllowed(resolvedType, knownType)) {
            return ValidationResult.valid();
        }
        return ValidationResult.invalid(String.format("Potential type name collision detected: '%s' bound to multiple types:" +
                        " %s (loaded by %s) and %s (loaded by %s)." +
                        " Assign unique names using the appropriate annotations or override the %s." +
                        " For details and solutions see %s." +
                        " If this warning is a false positive, please report it: %s.", namedType.getName(),
                knownType, getLoaderName(knownType), resolvedType, getLoaderName(resolvedType),
                TypeInfoGenerator.class.getSimpleName(), Urls.Errors.NON_UNIQUE_TYPE_NAME, Urls.ISSUES));
    }

    private AnnotatedType resolveType(Supplier<AnnotatedType> javaType) {
        return ClassUtils.normalize(javaType.get());
    }

    private boolean isMappingAllowed(AnnotatedType resolvedType, AnnotatedType knownType) {
        return resolvedType.equals(knownType) || typeComparator.compare(resolvedType, knownType) == 0;
    }

    private String getLoaderName(AnnotatedType type) {
        ClassLoader loader = ClassUtils.getRawType(type.getType()).getClassLoader();
        return loader != null ? loader.toString() : "the bootstrap class loader";
    }

    static class ValidationResult {

        private final String message;

        private ValidationResult(String message) {
            this.message = message;
        }

        static ValidationResult valid() {
            return new ValidationResult(null);
        }

        static ValidationResult invalid(String message) {
            return new ValidationResult(message);
        }

        boolean isValid() {
            return message == null;
        }

        String getMessage() {
            return message;
        }
    }
}
