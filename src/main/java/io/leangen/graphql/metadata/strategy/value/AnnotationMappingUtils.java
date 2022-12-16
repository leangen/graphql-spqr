package io.leangen.graphql.metadata.strategy.value;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.metadata.DefaultValue;
import io.leangen.graphql.metadata.exceptions.MappingException;
import io.leangen.graphql.util.ReservedStrings;
import io.leangen.graphql.util.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class AnnotationMappingUtils {

    public static String inputFieldName(Method method) {
        if (method.isAnnotationPresent(GraphQLInputField.class)) {
            return Utils.coalesce(method.getAnnotation(GraphQLInputField.class).name(), method.getName());
        }
        return method.getName();
    }

    public static String inputFieldDescription(Method method) {
        return method.isAnnotationPresent(GraphQLInputField.class) ? method.getAnnotation(GraphQLInputField.class).description() : "";
    }

    public static DefaultValue inputFieldDefaultValue(Method method) {
        validateNoConflictingDefaults(method);
        return DefaultValue.ofNullable(method.getDefaultValue()).map(AnnotationMappingUtils::destructureAnnotationArgument);
    }

    private static void validateNoConflictingDefaults(Method method) {
        Optional.ofNullable(method.getAnnotation(GraphQLInputField.class))
                .map(GraphQLInputField::defaultValue)
                .map(ReservedStrings::decodeDefault)
                .filter(DefaultValue::isSet)
                .ifPresent(def -> {
                    throw new MappingException(String.format(
                            "Annotation member %s.%s() must not have a default value specified via @%s",
                            method.getDeclaringClass().getName(), method.getName(), GraphQLInputField.class.getName()));
                });
    }

    public static Object inputFieldValue(Object value) {
        return destructureAnnotationArgument(value);
    }

    public static Object destructureAnnotationArgument(Object value) {
        if (value == null) return null;
        Class<?> clazz = value.getClass();
        if (clazz.isPrimitive() || GenericTypeReflector.isBoxType(clazz) || clazz == String.class || clazz.isEnum()) {
            return value;
        }
        if (clazz == Class.class) {
            return clazz.getName();
        }
        if (value instanceof Annotation) {
            return destructureAnnotation((Annotation) value);
        }
        if (clazz.isArray()) {
            return stream(((Object[]) value)).map(AnnotationMappingUtils::destructureAnnotationArgument).toArray();
        }
        throw new IllegalArgumentException(value + " is not a valid annotation argument");
    }

    public static Map<String, Object> destructureAnnotation(Annotation annotation) {
        return stream(annotation.annotationType().getDeclaredMethods())
                .filter(m -> m.getDeclaringClass() == annotation.annotationType())
                .collect(Collectors.toMap(AnnotationMappingUtils::inputFieldName, m -> invoke(annotation, m)));
    }

    private static Object invoke(Annotation annotation, Method method) {
        try {
            return destructureAnnotationArgument(method.invoke(annotation));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MappingException("Could not extract annotation argument values", e);
        }
    }
}
