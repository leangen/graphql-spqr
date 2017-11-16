package io.leangen.graphql.util;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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

    private static final Map<Class, List<Class>> implementationCache = new ConcurrentHashMap<>();
    private static final Class<?> javassistProxyClass;
    private static final String CGLIB_CLASS_SEPARATOR = "$$";
    private static final Set<Class> ROOT_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(Object.class, Annotation.class, Cloneable.class, Comparable.class, Serializable.class)));

    static {
        Class<?> proxy;
        try {
            proxy = ClassUtils.forName("javassist.util.proxy.ProxyObject");
        } catch (ClassNotFoundException e) {
            proxy = null;
        }
        javassistProxyClass = proxy;
    }

    /**
     * Retrieves all public methods on the given class (same as {@link Class#getMethods()}) annotated by the given annotation
     *
     * @param type       The class to scan
     * @param annotation The annotation to look for
     * @return All annotated methods
     */
    public static Set<Method> getAnnotatedMethods(final Class<?> type, final Class<? extends Annotation> annotation) {
        Set<Method> methods = new HashSet<>();
        collectPublicAbstractMethods(type, methods);
        Collections.addAll(methods, type.getMethods());
        return methods.stream()
                .filter(element -> element.isAnnotationPresent(annotation))
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves all public fields on the given class (same as {@link Class#getFields()}) annotated by the given annotation
     *
     * @param type       The class to scan
     * @param annotation The annotation to look for
     * @return All annotated fields
     */
    public static Set<Field> getAnnotatedFields(final Class<?> type, final Class<? extends Annotation> annotation) {
        return stream(type.getFields())
                .filter(element -> element.isAnnotationPresent(annotation))
                .collect(Collectors.toSet());
    }

    private static void collectPublicAbstractMethods(Class type, Set<Method> methods) {
        if (type == null || type.equals(Object.class)) {
            return;
        }
        if (isAbstract(type)) {
            Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .filter(method -> Modifier.isAbstract(method.getModifiers()))
                    .forEach(methods::add);
        }
        collectPublicAbstractMethods(type.getSuperclass(), methods);
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
        return GenericTypeReflector.getReturnType(method, declaringType);
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
        return GenericTypeReflector.getFieldType(field, declaringType);
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
        return GenericTypeReflector.getParameterTypes(executable, declaringType);
    }

    public static Class<?> getRawType(Type type) {
        Class<?> erased = GenericTypeReflector.erase(type);
        if (erased == Object.class && type != Object.class) {
            throw new TypeMappingException("Type of " + type.getTypeName() + " is lost to erasure. " +
                    "Consider explicitly providing the type to GraphQLSchemaGenerator#withOperationsFrom... " +
                    "methods, or customizing the mapping process.");
        }
        return erased;
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
    public static List<AnnotatedType> findImplementations(AnnotatedType superType, String... packages) {
        Class<?> rawType = getRawType(superType.getType());
        return findImplementations(rawType, packages).stream()
                .map(raw -> GenericTypeReflector.getExactSubType(superType, raw))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<Class> findImplementations(Class superType, String... packages) {
        if (implementationCache.containsKey(superType)) {
            return implementationCache.get(superType);
        }
        try {
            ClassFinder classFinder = new ClassFinder();
            packages = packages == null ? null : Arrays.stream(packages).filter(Utils::notEmpty).toArray(String[]::new);
            classFinder = packages == null || packages.length == 0 ? classFinder.addExplicitClassPath() : classFinder.add(superType.getClassLoader(), packages);
            List<Class> implementations = classFinder
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
     * @return The type of the same structure as the given type but with bounds erased, or the unchanged type itself if it contained no bounds
     */
    public static AnnotatedType eraseBounds(AnnotatedType type, AnnotatedType replacement) {
        if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type;
            AnnotatedType bound = wildcard.getAnnotatedLowerBounds().length > 0
                    ? eraseBounds(wildcard.getAnnotatedLowerBounds()[0], replacement)
                    : eraseBounds(wildcard.getAnnotatedUpperBounds()[0], replacement);
            if (bound.getType().equals(Object.class)) {
                if (replacement != null) {
                    bound = replacement;
                } else {
                    throw new TypeMappingException(type.getType());
                }
            }
            return GenericTypeReflector.updateAnnotations(bound, type.getAnnotations());
        }
        if (type instanceof AnnotatedTypeVariable) {
            AnnotatedType bound = ((AnnotatedTypeVariable) type).getAnnotatedBounds()[0];
            if (bound.getType().equals(Object.class)) {
                if (replacement != null) {
                    bound = replacement;
                } else {
                    throw new TypeMappingException(type.getType());
                }
            }
            return GenericTypeReflector.updateAnnotations(bound, type.getAnnotations());
        }
        if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) type;
            AnnotatedType[] typeArguments = Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .map(parameterType -> eraseBounds(parameterType, replacement))
                    .toArray(AnnotatedType[]::new);
            return GenericTypeReflector.replaceParameters(parameterizedType, typeArguments);
        }
        if (type instanceof AnnotatedArrayType) {
            return TypeFactory.arrayOf(eraseBounds(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(), replacement), type.getAnnotations());
        }
        return type;
    }
    
    public static AnnotatedType completeGenerics(AnnotatedType type, AnnotatedType replacement) {
        if (type.getType() instanceof Class) {
            Class clazz = (Class) type.getType();
            if (clazz.isArray()) {
                return TypeFactory.arrayOf(completeGenerics(GenericTypeReflector.annotate(clazz.getComponentType()), replacement), type.getAnnotations());
            } else {
                if (GenericTypeReflector.isMissingTypeParameters(clazz)) {
                    if (replacement == null) {
                        throw new TypeMappingException(clazz);
                    }
                    AnnotatedType[] parameters = new AnnotatedType[clazz.getTypeParameters().length];
                    for (int i = 0; i < parameters.length; i++) {
                        parameters[i] = replacement;
                    }
                    return TypeFactory.parameterizedAnnotatedClass(clazz, type.getAnnotations(), parameters);
                }
            }
        }
        else if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) type;
            AnnotatedType[] parameters = Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .map(parameterType -> completeGenerics(parameterType, replacement))
                    .toArray(AnnotatedType[]::new);
            return GenericTypeReflector.replaceParameters(parameterizedType, parameters);
        }
        else if (type instanceof AnnotatedArrayType) {
            AnnotatedType componentType = completeGenerics(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(), replacement);
            return TypeFactory.arrayOf(componentType, type.getAnnotations());
        }
        else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
            //can only happen if bounds haven't been erased (via eraseBounds) prior to invoking this method
            throw new TypeMappingException(type.getType().getTypeName() + " can not be completed. Call eraseBounds first?");
        }
        return type;
    }
    
    /**
     * Finds the most specific common super type of all the given types, merging the original annotations at each level.
     * If no common ancestors are found (except Object) a {@link TypeMappingException} is thrown.
     *
     * @param types Types whose most specific super types is to be found
     * @return The most specific super type
     */
    public static AnnotatedType getCommonSuperType(List<AnnotatedType> types) {
        return getCommonSuperType(types, new HashSet<>(), null);
    }

    /**
     * Finds the most specific common super type of all the given types, merging the original annotations at each level.
     * 
     * <p>If no common ancestors are found (except Object) returns {@code fallback} or throws a
     * {@link TypeMappingException} if {@code fallback} is {@code null}.</p>
     *
     * @param types Types whose most specific super types is to be found
     * @param fallback The type to return as the result when no common ancestors except Object are found (at any level)
     * 
     * @return The most specific super type
     */
    public static AnnotatedType getCommonSuperType(List<AnnotatedType> types, AnnotatedType fallback) {
        return getCommonSuperType(types, new HashSet<>(), fallback);
    }
    
    private static AnnotatedType getCommonSuperType(List<AnnotatedType> types, Set<String> seenTypeCombos, AnnotatedType fallback) {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("At least one type must be provided");
        }
        if (types.size() == 1) {
            return types.get(0);
        }
        Annotation[] mergedAnnotations = getMergedAnnotations(types.toArray(new AnnotatedType[types.size()]));
        if (types.stream().map(AnnotatedType::getType).allMatch(type -> type.equals(types.get(0).getType()))) {
            return GenericTypeReflector.replaceAnnotations(types.get(0), mergedAnnotations);
        }
        List<Class<?>> classes = types.stream().map(AnnotatedType::getType).map(ClassUtils::getRawType).collect(Collectors.toList());
        String typeNames = types.stream().map(type -> type.getType().getTypeName()).sorted().collect(Collectors.joining(","));
        if (seenTypeCombos.contains(typeNames)) {
            return fallbackOrException(fallback);
        }
        seenTypeCombos.add(typeNames);

        //deal with arrays first as they are special
        if (types.stream().allMatch(type -> type instanceof AnnotatedArrayType)) {
            List<AnnotatedType> componentTypes = types.stream()
                    .map(type -> ((AnnotatedArrayType) type).getAnnotatedGenericComponentType())
                    .collect(Collectors.toList());
            AnnotatedType componentType = getCommonSuperType(componentTypes, seenTypeCombos, fallback);
            return TypeFactory.arrayOf(componentType, mergedAnnotations);
        }

        Class<?> commonRawSuperType = getCommonSuperTypes(classes).get(0);
        if (classes.stream().noneMatch(ROOT_TYPES::contains) && ROOT_TYPES.contains(commonRawSuperType)) {
            return fallbackOrException(fallback);
        }
        List<AnnotatedType> normalizedTypes = types.stream()
                .map(type -> GenericTypeReflector.getExactSuperType(type, commonRawSuperType))
                .collect(Collectors.toList());
        if (normalizedTypes.stream().anyMatch(type -> GenericTypeReflector.isMissingTypeParameters(type.getType()))) {
            throw new TypeMappingException("Automatic type inference failed because some of the types are missing generic type parameter(s).");
        }
        if (normalizedTypes.stream().allMatch(type -> type.getType() instanceof Class)) {
            return annotate(commonRawSuperType, mergedAnnotations);
        }
        if (normalizedTypes.stream().allMatch(type -> type instanceof AnnotatedParameterizedType)) {
            AnnotatedType[] parameters = Arrays.stream(commonRawSuperType.getTypeParameters())
                    .map(param -> normalizedTypes.stream().map(type -> GenericTypeReflector.getTypeParameter(type, param)).collect(Collectors.toList()))
                    .map(paramTypes -> getCommonSuperType(paramTypes, seenTypeCombos, fallback))
                    .toArray(AnnotatedType[]::new);
            return TypeFactory.parameterizedAnnotatedClass(commonRawSuperType, mergedAnnotations, parameters);
        }
        return fallbackOrException(fallback);
    }

    /**
     * @param classes Types whose most specific super types is to be found
     * @return The most specific super type
     * @see ClassUtils#getCommonSuperType(List)
     */
    public static List<Class<?>> getCommonSuperTypes(List<Class<?>> classes) {
        // start off with set from first hierarchy
        Set<Class<?>> rollingIntersect = new LinkedHashSet<>(
                getSuperTypes(classes.get(0)));
        // intersect with next
        for (int i = 1; i < classes.size(); i++) {
            rollingIntersect.retainAll(getSuperTypes(classes.get(i)));
        }
        if (rollingIntersect.isEmpty()) {
            return Collections.singletonList(Object.class);
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

    private static AnnotatedType fallbackOrException(AnnotatedType fallback) {
        if (fallback != null) {
            return fallback;
        }
        throw new TypeMappingException("Automatic type inference failed because some of the types had no common ancestors except for Object class");
    }
    
    /**
     * Attempts to discover if the given class is a dynamically generated proxy class.
     * Standard Java proxies, cglib and Javassist proxies are detected.
     * @param clazz The class to test
     * @return {@code true} if the given class is a known proxy, {@code false} otherwise
     */
    public static boolean isProxy(Class<?> clazz) {
        return Proxy.isProxyClass(clazz)
                || (javassistProxyClass != null && javassistProxyClass.isAssignableFrom(clazz))
                || clazz.getName().contains(CGLIB_CLASS_SEPARATOR); //cglib
    }

    public static Class<?> forName(String className) throws ClassNotFoundException {
        return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
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
            return forName(className);
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
