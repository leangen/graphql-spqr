package io.leangen.graphql.util;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.util.classpath.ClassFinder;
import io.leangen.graphql.util.classpath.ClassReadingException;
import io.leangen.graphql.util.classpath.SubclassClassFilter;

import static io.leangen.geantyref.GenericTypeReflector.annotate;
import static io.leangen.geantyref.GenericTypeReflector.capture;
import static io.leangen.geantyref.GenericTypeReflector.merge;
import static java.util.Arrays.stream;

public class ClassUtils {

    private static Map<Class, Collection<Class>> implementationCache = new ConcurrentHashMap<>();

    /**
     * Retrieves all public methods on the given class (same as {@link Class#getMethods()}) annotated by the given annotation
     *
     * @param type       The class to scan
     * @param annotation The annotation to look for
     * @return All annotated methods
     */
    public static Set<Method> getAnnotatedMethods(final Class<?> type, final Class<? extends Annotation> annotation) {
        return getAnnotatedElements(type.getMethods(), annotation);
    }

    /**
     * Retrieves all public fields on the given class (same as {@link Class#getFields()}) annotated by the given annotation
     *
     * @param type       The class to scan
     * @param annotation The annotation to look for
     * @return All annotated fields
     */
    public static Set<Field> getAnnotatedFields(final Class<?> type, final Class<? extends Annotation> annotation) {
        return getAnnotatedElements(type.getFields(), annotation);
    }

    private static <T extends AnnotatedElement> Set<T> getAnnotatedElements(final T[] annotatedElements, final Class<? extends Annotation> annotation) {
        return stream(annotatedElements)
                .filter(element -> element.isAnnotationPresent(annotation))
                .collect(Collectors.toSet());
    }

    /**
     * Returns the exact annotated return type of the method declared by the given type, with type variables resolved (if possible)
     *
     * @param method        The method whose return type is to be resolved
     * @param declaringType The declaring annotated type against which to resolve the return type
     * @return The resolved annotated return type
     */
    public static AnnotatedType getReturnType(Method method, AnnotatedType declaringType) {
        AnnotatedType exactDeclaringType = GenericTypeReflector.getExactSuperType(capture(declaringType), method.getDeclaringClass());
        if (GenericTypeReflector.isMissingTypeParameters(exactDeclaringType.getType())) {
            return method.getAnnotatedReturnType();
        }
        return GenericTypeReflector.getExactReturnType(method, declaringType);
    }

    /**
     * Returns the exact annotated type of the field declared by the given type, with type variables resolved (if possible)
     *
     * @param field         The field whose type is to be resolved
     * @param declaringType The declaring annotated type against which to resolve the field type
     * @return The resolved annotated field type
     */
    public static AnnotatedType getFieldType(Field field, AnnotatedType declaringType) {
        AnnotatedType exactDeclaringType = GenericTypeReflector.getExactSuperType(capture(declaringType), field.getDeclaringClass());
        if (GenericTypeReflector.isMissingTypeParameters(exactDeclaringType.getType())) {
            return field.getAnnotatedType();
        }
        return GenericTypeReflector.getExactFieldType(field, declaringType);
    }

    public static AnnotatedType[] getTypeArguments(AnnotatedType type) {
        if (type instanceof AnnotatedParameterizedType) {
            return ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments();
        } else {
            throw new IllegalArgumentException("Raw parameterized types are not possible to map: " + type.getType().getTypeName());
        }
    }

    /**
     * Returns the exact annotated parameter types of the executable declared by the given type, with type variables resolved (if possible)
     *
     * @param executable    The executable whose parameter types are to be resolved
     * @param declaringType The declaring annotated type against which to resolve the types of the parameters of the given executable
     * @return The resolved annotated types of the parameters of the given executable
     */
    public static AnnotatedType[] getParameterTypes(Executable executable, AnnotatedType declaringType) {
        AnnotatedType exactDeclaringType = GenericTypeReflector.getExactSuperType(capture(declaringType), executable.getDeclaringClass());
        if (GenericTypeReflector.isMissingTypeParameters(exactDeclaringType.getType())) {
            return executable.getAnnotatedParameterTypes();
        }
        return GenericTypeReflector.getExactParameterTypes(executable, declaringType);
    }

    public static Class<?> getRawType(Type type) {
        Class<?> erased = GenericTypeReflector.erase(type);
        //TODO This should preferably be a warning, not an exception, or have customizable behavior
        if (erased == Object.class && type != Object.class) {
            throw new IllegalArgumentException("Type " + type.getTypeName() + " is lost due to erasure");
        }
        return erased;
    }

    /**
     * Check whether the member has resolvable type. A convenience method for easier fail-fast logic.
     *
     * @param declaringType The type declaring the member (against which the member's type will be resolved)
     * @param member        The field of method to be checked
     */
    public static void checkIfResolvable(AnnotatedType declaringType, Member member) {
        try {
            if (declaringType instanceof AnnotatedParameterizedType) {
                getTypeArguments(declaringType);
            }
            getRawType(declaringType.getType());
        } catch (IllegalArgumentException e) {
            throw new TypeMappingException(member, e);
        }
    }

    /**
     * Checks whether the given method is a JavaBean property getter
     *
     * @param getter The method to be checked
     * @return Boolean indicating whether the method is a getter
     * @see ClassUtils#isSetter(Method)
     */
    public static boolean isGetter(Method getter) {
        return getter.getParameterCount() == 0 && getter.getReturnType() != void.class
                && getter.getReturnType() != Void.class && getter.getName().startsWith("get") ||
                ((getter.getReturnType() == Boolean.class || getter.getReturnType() == boolean.class)
                        && getter.getName().startsWith("is"));
    }

    /**
     * Checks whether the given method is a JavaBean property setter
     *
     * @param setter The method to be checked
     * @return Boolean indicating whether the method is a setter
     * @see ClassUtils#isGetter(Method)
     */
    public static boolean isSetter(Method setter) {
        return setter.getName().startsWith("set") && setter.getParameterCount() == 1;
    }

    public static String getFieldNameFromGetter(Method getter) {
        return Introspector.decapitalize(getter.getName().replaceAll("^get", "").replaceAll("^is", ""));
    }

    public static String getFieldNameFromSetter(Method setter) {
        return Introspector.decapitalize(setter.getName().replaceAll("^set", ""));
    }

    public static Method findGetter(Class<?> type, String fieldName) throws NoSuchMethodException {
        try {
            return type.getMethod("get" + capitalize(fieldName));
        } catch (NoSuchMethodException e) { /*no-op*/}
        return type.getMethod("is" + capitalize(fieldName));
    }

    public static Method findSetter(Class<?> type, String fieldName, Class<?> fieldType) throws NoSuchMethodException {
        return type.getMethod("set" + capitalize(fieldName), fieldType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object source, String fieldName) {
        try {
            try {
                return (T) findGetter(source.getClass(), fieldName).invoke(source);
            } catch (NoSuchMethodException e) {
                return (T) source.getClass().getField(fieldName).get(source);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to extract the value of field " + fieldName + " from the given instance of " + source.getClass(), e);
        }
    }

    /**
     * Scans classpath for implementations/subtypes of the given {@link AnnotatedType}. Only the matching classes are loaded.
     *
     * @param superType The type whose implementations/subtypes are to be looked for
     * @return A collection of {@link AnnotatedType}s found on the classpath that are implementations/subtypes of {@code superType}
     * @throws RuntimeException If a class file could not be parsed or a class could not be loaded
     */
    public static Collection<AnnotatedType> findImplementations(AnnotatedType superType, String... packages) {
        Class<?> rawType = getRawType(superType.getType());
        return findImplementations(rawType, packages).stream()
                .map(raw -> GenericTypeReflector.getExactSubType(superType, raw))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Collection<Class> findImplementations(Class superType, String... packages) {
        if (implementationCache.containsKey(superType)) {
            return implementationCache.get(superType);
        }
        try {
            ClassFinder classFinder = new ClassFinder();
            classFinder = packages == null || packages.length == 0 ? classFinder.addExplicitClassPath() : classFinder.add(superType.getClassLoader(), packages);
            Collection<Class> implementations = classFinder
                    .findClasses(new SubclassClassFilter(superType)).stream()
                    .map(classInfo -> loadClass(classInfo.getClassName()))
                    .collect(Collectors.toList());
            implementationCache.putIfAbsent(superType, implementations);
            return implementations;
        } catch (ClassReadingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAbstract(AnnotatedType type) {
        return isAbstract(getRawType(type.getType()));
    }

    public static boolean isAbstract(Class<?> type) {
        return (type.isInterface() || Modifier.isAbstract(type.getModifiers())) &&
                !type.isPrimitive() && !type.isArray();
    }

    public static boolean isAssignable(Type superType, Type subType) {
        return (((superType instanceof ParameterizedType
                && Arrays.stream(((ParameterizedType) superType).getActualTypeArguments())
                .allMatch(arg -> arg instanceof TypeVariable))
                || (superType instanceof GenericArrayType &&
                ((GenericArrayType) superType).getGenericComponentType() instanceof TypeVariable))
                && ClassUtils.getRawType(superType).isAssignableFrom(ClassUtils.getRawType(subType)))
                || (superType == Byte.class && subType == byte.class)
                || (superType == Short.class && subType == short.class)
                || (superType == Integer.class && subType == int.class)
                || (superType == Long.class && subType == long.class)
                || (superType == Float.class && subType == float.class)
                || (superType == Double.class && subType == double.class)
                || (superType == Boolean.class && subType == boolean.class)
                || (superType == Void.class && subType == void.class)
                || GenericTypeReflector.isSuperType(superType, subType);
    }

    public static String toString(AnnotatedType type) {
        return type.getType().getTypeName() + "(" + Arrays.toString(type.getAnnotations()) + ")";
    }

    public static boolean containsTypeAnnotation(AnnotatedType type, Class<? extends Annotation> annotation) {
        if (type.isAnnotationPresent(annotation)) {
            return true;
        }
        if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = ((AnnotatedParameterizedType) type);
            return Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .anyMatch(param -> containsTypeAnnotation(param, annotation));
        }
        if (type instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable variable = ((AnnotatedTypeVariable) type);
            return Arrays.stream(variable.getAnnotatedBounds())
                    .anyMatch(bound -> containsTypeAnnotation(bound, annotation));
        }
        if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wildcard = ((AnnotatedWildcardType) type);
            return Stream.concat(
                    Arrays.stream(wildcard.getAnnotatedLowerBounds()),
                    Arrays.stream(wildcard.getAnnotatedUpperBounds()))
                    .anyMatch(param -> containsTypeAnnotation(param, annotation));
        }
        if (type instanceof AnnotatedArrayType) {
            return containsTypeAnnotation(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(), annotation);
        }
        return false;
    }

    public static Annotation[] getAllAnnotations(Stream<AnnotatedType> types) {
        return types
                .flatMap(type -> Arrays.stream(type.getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
    }

    public static AnnotatedType addAnnotations(AnnotatedType type, Annotation[] annotations) {
        if (type == null || annotations == null || annotations.length == 0) return type;
        return GenericTypeReflector.updateAnnotations(type, merge(type.getAnnotations(), annotations));
    }

    /**
     * Recursively replaces all bounded types found within the structure of the given {@link AnnotatedType} with their first bound.
     * I.e.
     * <ul>
     *     <li>All {@link AnnotatedWildcardType}s are replaced with their first lower bound if it exists,
     *     or their first upper bound otherwise. All annotations are preserved.</li>
     *     <li>All {@link AnnotatedTypeVariable}s are replaced with their first bound. All annotations are preserved.</li>
     *     <li>Other types are kept as they are.</li>
     * </ul>
     *
     * @param type A potentially bounded type
     * @return The first bound of bounded types, or the unchanged type itself
     */
    public static AnnotatedType stripBounds(AnnotatedType type) {
        if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type;
            AnnotatedType bound = wildcard.getAnnotatedLowerBounds().length > 0
                    ? stripBounds(wildcard.getAnnotatedLowerBounds()[0])
                    : stripBounds(wildcard.getAnnotatedUpperBounds()[0]);
            return type.getAnnotations().length > 0 ? GenericTypeReflector.replaceAnnotations(bound, getMergedAnnotations(type, bound)) : bound;
        }
        if (type instanceof AnnotatedTypeVariable) {
            AnnotatedType bound = ((AnnotatedTypeVariable) type).getAnnotatedBounds()[0];
            return type.getAnnotations().length > 0 ? GenericTypeReflector.replaceAnnotations(bound, getMergedAnnotations(type, bound)) : bound;
        }
        if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) type;
            AnnotatedType[] typeArguments = Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .map(ClassUtils::stripBounds)
                    .toArray(AnnotatedType[]::new);
            return GenericTypeReflector.replaceParameters(parameterizedType, typeArguments);
        }
        if (type instanceof AnnotatedArrayType) {
            return TypeFactory.arrayOf(stripBounds(((AnnotatedArrayType) type).getAnnotatedGenericComponentType()), type.getAnnotations());
        }
        return type;
    }

    /**
     * Finds the most specific super type of all given types. Currently broken, so it is expected all provided types are actually the same.
     *
     * @param types Types whose most specific super types is to be found
     * @return The most specific super type
     */
    public static AnnotatedType getCommonSuperType(List<AnnotatedType> types) {
        if (types.isEmpty()) {
            throw new IllegalArgumentException("At least one class must be provided");
        }
        if (types.stream().allMatch(type -> GenericTypeReflector.equals(type, types.get(0)))) return types.get(0);
        List<Class> classes = types.stream().map(AnnotatedType::getType).map(ClassUtils::getRawType).collect(Collectors.toList());
        return annotate(getCommonSuperTypes(classes).get(0), getMergedAnnotations(types.toArray(new AnnotatedType[types.size()])));
    }

    /**
     * @param classes Types whose most specific super types is to be found
     * @return The most specific super type
     * @see ClassUtils#getCommonSuperType(List)
     */
    public static List<Class<?>> getCommonSuperTypes(List<Class> classes) {
        // start off with set from first hierarchy
        Set<Class<?>> rollingIntersect = new LinkedHashSet<>(
                getSuperTypes(classes.get(0)));
        // intersect with next
        for (int i = 1; i < classes.size(); i++) {
            rollingIntersect.retainAll(getSuperTypes(classes.get(i)));
        }
        List<Class<?>> result = new LinkedList<>(rollingIntersect);
        result.sort(new TypeComparator());
        return result;
    }

    public static Set<Class<?>> getSuperTypes(Class<?> clazz) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        Set<Class<?>> nextLevel = new LinkedHashSet<>();
        nextLevel.add(clazz);
        do {
            classes.addAll(nextLevel);
            Set<Class<?>> thisLevel = new LinkedHashSet<>(nextLevel);
            nextLevel.clear();
            for (Class<?> each : thisLevel) {
                Class<?> superClass = each.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    nextLevel.add(superClass);
                }
                Collections.addAll(nextLevel, each.getInterfaces());
            }
        } while (!nextLevel.isEmpty());
        return classes;
    }

    private static String capitalize(final String str) {
        final char firstChar = str.charAt(0);
        final char newChar = Character.toUpperCase(firstChar);
        if (firstChar == newChar) {
            // already capitalized
            return str;
        }

        char[] newChars = new char[str.length()];
        newChars[0] = newChar;
        str.getChars(1, str.length(), newChars, 1);
        return String.valueOf(newChars);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an array containing all annotations declared by the given annotated types, without duplicates.
     *
     * @param types Annotated types whose annotations are to be extracted and merged
     * @return An array containing all annotations declared by the given annotated types, without duplicates
     */
    private static Annotation[] getMergedAnnotations(AnnotatedType... types) {
        return Arrays.stream(types)
                .flatMap(type -> Arrays.stream(type.getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
    }

    private static class TypeComparator implements Comparator<Class<?>> {

        @Override
        public int compare(Class<?> c1, Class<?> c2) {
            if (c2 == Cloneable.class || c2 == Serializable.class) {
                return -1;
            }
            if (!c1.isInterface() && c2.isInterface()) {
                return -1;
            }
            if (c2.isAssignableFrom(c1)) {
                return -1;
            }
            return 0;
        }
    }
}
