package io.leangen.graphql.util;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.util.classpath.ClassFinder;
import io.leangen.graphql.util.classpath.ClassInfo;
import io.leangen.graphql.util.classpath.ClassReadingException;
import io.leangen.graphql.util.classpath.SubclassClassFilter;
import sun.misc.Unsafe;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static io.leangen.gentyref8.GenericTypeReflector.annotate;
import static io.leangen.gentyref8.GenericTypeReflector.capture;
import static java.util.Arrays.stream;

/**
 * Created by bojan.tomic on 3/2/16.
 */
public class ClassUtils {

    private static Unsafe unsafe;
    private static TypeResolver typeResolver = new TypeResolver();

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            //TODO log warning
        }
    }

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

    //TODO Return multiple types here e.g. for maps and "union" types for wildcards and variables
    public static AnnotatedType[] getTypeArguments(AnnotatedType type) {
        if (type instanceof AnnotatedParameterizedType) {
            return ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments();
        } else {
            throw new IllegalArgumentException("Raw parameterized types are not possible to map: " + type.getType().getTypeName());
        }
    }

    /**
     * Returns the exact annotated parameters types of the method declared by the given type, with type variables resolved (if possible)
     *
     * @param method        The method whose parameter types are to be resolved
     * @param declaringType The declaring annotated type against which to resolve the types of the parameters of the given method
     * @return The resolved annotated types of the parameters of the given method
     */
    public static AnnotatedType[] getParameterTypes(Method method, AnnotatedType declaringType) {
        AnnotatedType exactDeclaringType = GenericTypeReflector.getExactSuperType(capture(declaringType), method.getDeclaringClass());
        if (GenericTypeReflector.isMissingTypeParameters(exactDeclaringType.getType())) {
            return method.getAnnotatedParameterTypes();
        }
        return GenericTypeReflector.getExactParameterTypes(method, declaringType);
    }

    public static Class getRawType(Type type) {
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
            getTypeArguments(declaringType);
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
        return getter.getName().startsWith("get") ||
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
        return setter.getName().startsWith("set");
    }

    public static String getFieldNameFromGetter(Method getter) {
        return Introspector.decapitalize(getter.getName().replaceAll("^get", "").replaceAll("^is", ""));
    }

    public static String getFieldNameFromSetter(Method setter) {
        return Introspector.decapitalize(setter.getName().replaceAll("^set", ""));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object source, String fieldName) {
        try {
            try {
                //TODO handle "is" for booleans
                return (T) source.getClass().getMethod("get" + capitalize(fieldName)).invoke(source);
            } catch (NoSuchMethodException e) {
                return (T) source.getClass().getField(fieldName).get(source);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to extract the value of field " + fieldName + " from the given instance of " + source.getClass(), e);
        }
    }

    /**
     * Scans classpath for implementations/subtypes of the given Type. Only the matching classes are loaded.
     *
     * @param superType The type whose implementations/subtypes are to be looked for
     * @return A collection of <tt>Type</tt>s found on the classpath that are implementations/subtypes of <tt>superType</tt>
     * @throws ClassReadingException
     */
    public static Collection<Type> findImplementations(Type superType) throws ClassReadingException {
        Collection<ClassInfo> rawImpls = new ClassFinder()
                .addPrunedClassPath()
                .findClasses(new SubclassClassFilter(getRawType(superType)));


        ResolvedType resolvedSuperType = typeResolver.resolve(superType);
        return rawImpls.stream()
                .map(classInfo -> loadClass(classInfo.getClassName()))
                .map(raw -> {
                    try {
                        return typeResolver.resolveSubtype(resolvedSuperType, raw);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(impl -> impl != null)
                .collect(Collectors.toList());
    }

    public static boolean isSuperType(Type superType, Type subType) {
        return GenericTypeReflector.isSuperType(superType, subType);
    }

    /**
     * Returns the first bound of bounded types, or the unchanged type itself.
     * If the given type is an AnnotatedWildcardType its first lower bound is returned if it exists, or it's first upper bound otherwise.
     * If the type is an AnnotatedTypeVariable, its first bound is returned. In all other cases, the unchanged type itself is returned.
     *
     * @param type A potentially bounded type
     * @return The first bound of bounded types, or the unchanged type itself
     */
    public static AnnotatedType stripBounds(AnnotatedType type) {
        if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type;
            AnnotatedType bound = wildcard.getAnnotatedLowerBounds().length > 0
                    ? wildcard.getAnnotatedLowerBounds()[0]
                    : wildcard.getAnnotatedUpperBounds()[0];
            return type.getAnnotations().length > 0 ? GenericTypeReflector.replaceAnnotations(bound, getMergedAnnotations(type, bound)) : bound;
        }
        if (type instanceof AnnotatedTypeVariable) {
            AnnotatedType bound = ((AnnotatedTypeVariable) type).getAnnotatedBounds()[0];
            return type.getAnnotations().length > 0 ? GenericTypeReflector.replaceAnnotations(bound, getMergedAnnotations(type, bound)) : bound;
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
        if (types.stream().allMatch(type -> type.equals(types.get(0)))) return types.get(0);
        List<Class> classes = types.stream().map(AnnotatedType::getType).map(ClassUtils::getRawType).collect(Collectors.toList());
        return annotate(getCommonSuperTypes(classes).get(0));
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
        Collections.sort(result, new TypeComparator());
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

    /**
     * Allocates an instance of the required class, skipping all constructors. To be used only when no other option is available.
     *
     * @param type The class representation of the instance type
     * @param <T>  The instance type
     * @return Bare instance, potentially not fully initialized
     * @throws InstantiationException If an instance of <tt>Unsafe<tt/> could not be acquired
     */
    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<T> type) throws InstantiationException {
        if (unsafe == null) {
            throw new InstantiationException("Unsafe is unavailable. Instance allocation failed.");
        }
        return (T) unsafe.allocateInstance(type);
    }

    /**
     * Returns an array containing all annotations declared by the given annotated types
     *
     * @param types Annotated types whose annotations are to be extracted and merged
     * @return An array containing all annotations declared by the given annotated types
     */
    private static Annotation[] getMergedAnnotations(AnnotatedType... types) {
        return Arrays.stream(types)
                .flatMap(type -> Arrays.stream(type.getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
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
