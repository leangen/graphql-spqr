package io.leangen.graphql.util;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.value.Property;

import java.beans.Introspector;
import java.io.Closeable;
import java.io.Externalizable;
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
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.leangen.geantyref.GenericTypeReflector.annotate;
import static io.leangen.geantyref.GenericTypeReflector.capture;
import static io.leangen.geantyref.GenericTypeReflector.merge;
import static java.util.Arrays.stream;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ClassUtils {

    private static final Class<?> javassistProxyClass;
    private static final List<String> KNOWN_PROXY_CLASS_SEPARATORS = Arrays.asList("$$", "$ByteBuddy$", "$HibernateProxy$");
    private static final List<Class<?>> ROOT_TYPES = Arrays.asList(
            Object.class, Annotation.class, Cloneable.class, Comparable.class, Externalizable .class, Serializable.class,
            Closeable.class, AutoCloseable.class);
    private static final Map<Class<?>, Object> DEFAULT_PRIMITIVE_VALUES;

    static {
        Class<?> proxy;
        try {
            proxy = ClassUtils.forName("javassist.util.proxy.ProxyObject");
        } catch (ClassNotFoundException e) {
            proxy = null;
        }
        javassistProxyClass = proxy;

        Map<Class<?>, Object> defaultPrimitives = new HashMap<>();
        defaultPrimitives.put(boolean.class, false);
        defaultPrimitives.put(byte.class, 0);
        defaultPrimitives.put(char.class, '\u0000');
        defaultPrimitives.put(double.class, 0.0d);
        defaultPrimitives.put(float.class, 	0.0f);
        defaultPrimitives.put(int.class, 0);
        defaultPrimitives.put(long.class, 0L);
        defaultPrimitives.put(short.class, 0);
        DEFAULT_PRIMITIVE_VALUES = Collections.unmodifiableMap(defaultPrimitives);
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

    public static Set<Property> getProperties(final Class<?> type) {
        return stream(type.getMethods())
                .filter(ClassUtils::isGetter)
                .map(getter -> findFieldByGetter(getter)
                        .map(field -> new Property(field, getter))
                        .filter(prop -> prop.getField().getType().equals(prop.getGetter().getReturnType()))
                        .filter(prop -> !Modifier.isPublic(prop.getField().getModifiers()))
                        .filter(prop -> !Modifier.isAbstract(prop.getGetter().getModifiers())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private static void collectPublicAbstractMethods(Class<?> type, Set<Method> methods) {
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
        if (isMissingTypeParameters(exactDeclaringType.getType())) {
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
        if (isMissingTypeParameters(exactDeclaringType.getType())) {
            return field.getAnnotatedType();
        }
        return GenericTypeReflector.getFieldType(field, declaringType);
    }

    public static Field getEnumConstantField(Enum<?> constant) {
        try {
            return constant.getClass().getField(constant.name());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
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
        if (isMissingTypeParameters(exactDeclaringType.getType())) {
            return executable.getAnnotatedParameterTypes();
        }
        return GenericTypeReflector.getParameterTypes(executable, declaringType);
    }

    public static <T> Class<T> getRawType(Type type) {
        @SuppressWarnings("unchecked") Class<T> erased = (Class<T>) GenericTypeReflector.erase(type);
        if (erased == Object.class && type != Object.class) {
            throw new TypeMappingException("Type of " + type.getTypeName() + " is lost to erasure. " +
                    "Consider explicitly providing the type to GraphQLSchemaGenerator#withOperationsFrom... " +
                    "methods, or customizing the mapping process.");
        }
        return erased;
    }

    // The reason this exists is to allow for potential additional checks that GeAnTyRef doesn't perform,
    // like whether the given ParameterizedType has unresolved wildcards and/or variables
    public static boolean isMissingTypeParameters(Type type) {
        return GenericTypeReflector.isMissingTypeParameters(type);
    }

    public static <T extends AnnotatedType> T normalize(T type) {
        type = GenericTypeReflector.toCanonicalBoxed(type);
        if (Arrays.stream(type.getAnnotations()).anyMatch(ann -> ann.annotationType().equals(GraphQLUnion.class))) {
            type = removeAnnotations(type, Collections.singleton(GraphQLUnion.class));
        }
        return type;
    }

    public static <T> T instance(AnnotatedType type) {
        return instance(getRawType(type.getType()));
    }

    public static <T> T instance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T instanceWithOptionalInjection(Class<T> clazz, Object... arguments) {
        Class<?>[] parameterTypes = stream(arguments).map(Object::getClass).toArray(Class<?>[]::new);
        return instanceWithOptionalInjection(clazz, parameterTypes, arguments);
    }

    public static <T> T instanceWithOptionalInjection(Class<T> clazz, Class<?> parameterType, Object argument) {
        return instanceWithOptionalInjection(clazz, new Class<?>[]{parameterType}, new Object[]{argument});
    }

    public static <T> T instanceWithOptionalInjection(Class<T> clazz, Class<?>[] parameterTypes, Object[] arguments) {
        try {
            try {
                return clazz.getDeclaredConstructor(parameterTypes).newInstance(arguments);
            } catch (NoSuchMethodException e) {
                return clazz.getDeclaredConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            String argumentTypes = stream(parameterTypes).map(Class::getName).collect(Collectors.joining(","));
            throw new IllegalArgumentException(
                   clazz.getName() + " must expose a public default constructor, or a constructor accepting " + argumentTypes, e);
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
        return isReal(getter) && getter.getParameterCount() == 0 && getter.getReturnType() != void.class
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
        return isReal(setter) && setter.getName().startsWith("set") && setter.getParameterCount() == 1;
    }

    public static boolean isReal(Method method) {
        return !method.isBridge() && !method.isSynthetic();
    }

    public static boolean isReal(Field field) {
        return !field.isSynthetic();
    }

    public static boolean isReal(Parameter parameter) {
        return !parameter.isImplicit() && !parameter.isSynthetic();
    }

    public static boolean isReal(Member member) {
        Objects.requireNonNull(member, "Member must not be null");
        if (member instanceof Method) {
            return isReal((Method) member);
        }
        if (member instanceof Field) {
            return isReal(((Field) member));
        }
        return member.isSynthetic();
    }

    public static boolean isReal(AnnotatedElement element) {
        Objects.requireNonNull(element, "Element must not be null");
        if (element instanceof Member) {
            return isReal(((Member) element));
        }
        if (element instanceof Parameter) {
            return isReal(((Parameter) element));
        }
        throw new IllegalArgumentException("Can not determine if an element of type " + element.getClass().getName() + " is real");
    }

    public static String getFieldNameFromGetter(Method getter) {
        String name = getter.getName();
        if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            name = getter.getName().replaceAll("^get", "");
        } else if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
            name = getter.getName().replaceAll("^is", "");
        }
        return Introspector.decapitalize(name);
    }

    public static String getFieldNameFromSetter(Method setter) {
        return Introspector.decapitalize(setter.getName().replaceAll("^set", ""));
    }

    public static <T extends Member & AnnotatedElement> List<AnnotatedElement> getPropertyMembers(T member) {
        List<AnnotatedElement> propertyElements = new ArrayList<>(3);
        if (member instanceof Field) {
            findSetter(member.getDeclaringClass(), member.getName(), ((Field) member).getType()).ifPresent(propertyElements::add);
            findGetter(member.getDeclaringClass(), member.getName()).ifPresent(propertyElements::add);
            propertyElements.add(member);
        }
        if (member instanceof Method && isGetter((Method) member)) {
            Method getter = (Method) member;
            findSetter(getter.getDeclaringClass(), getFieldNameFromGetter(getter), getter.getReturnType()).ifPresent(propertyElements::add);
            propertyElements.add(getter);
            findFieldByGetter(getter).ifPresent(propertyElements::add);
        }
        if (member instanceof Method && isSetter((Method) member)) {
            Method setter = (Method) member;
            propertyElements.add(setter);
            findGetter(setter.getDeclaringClass(), getFieldNameFromSetter(setter)).ifPresent(propertyElements::add);
            findFieldBySetter(setter).ifPresent(propertyElements::add);
        }
        return propertyElements;
    }

    public static Optional<Method> findGetter(Class<?> type, String fieldName) {
        String propertyName = Utils.capitalize(fieldName);
        Optional<Method> getter = findMethod(type, "get" + propertyName);
        if (getter.isPresent()) {
            return getter;
        }
        return findMethod(type, "is" + propertyName);
    }

    public static Optional<Method> findSetter(Class<?> type, String fieldName, Class<?> fieldType) {
        return findMethod(type, "set" + Utils.capitalize(fieldName), fieldType);
    }

    public static Optional<Field> findFieldByGetter(Method getter) {
        return findField(getter.getDeclaringClass(), getFieldNameFromGetter(getter));
    }

    public static Optional<Field> findFieldBySetter(Method setter) {
        return findField(setter.getDeclaringClass(), getFieldNameFromSetter(setter));
    }

    public static Optional<Field> findField(Class<?> type, String fieldName) {
        return findField(type, field -> field.getName().equals(fieldName));
    }

    public static Optional<Field> findField(Class<?> type, Predicate<Field> condition) {
        if (type.isInterface()) {
            return Optional.empty();
        }
        while (!type.equals(Object.class)) {
            Optional<Field> match = stream(type.getDeclaredFields()).filter(condition).findFirst();
            if (match.isPresent()) {
                return match;
            }
            type = type.getSuperclass();
        }
        return Optional.empty();
    }

    public static Optional<Method> findMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return Optional.of(type.getMethod(methodName, parameterTypes));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object source, String fieldName) {
        try {
            Optional<Method> getter = findGetter(source.getClass(), fieldName);
            if (getter.isPresent()) {
                return (T) getter.get().invoke(source);
            } else {
                return (T) source.getClass().getField(fieldName).get(source);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to extract the value of field " + fieldName + " from the given instance of " + source.getClass(), e);
        }
    }

    /**
     * Searches for the implementations/subtypes of the given {@link AnnotatedType}. Only the matching classes are loaded.
     *
     * @param superType The type the implementations/subtypes of which are to be searched for
     * @param packages The packages to limit the search to
     *
     * @return A collection of {@link AnnotatedType}s discovered that implementation/extend {@code superType}
     *
     * @deprecated Use {@link ClassFinder} directly as that enables caching of the search results
     */
    @Deprecated
    public static List<AnnotatedType> findImplementations(AnnotatedType superType, String... packages) {
        return new ClassFinder().findImplementations(superType, info -> true, false, packages);
    }

    public static boolean isAbstract(AnnotatedType type) {
        return isAbstract(getRawType(type.getType()));
    }

    public static boolean isAbstract(Class<?> type) {
        return (type.isInterface() || Modifier.isAbstract(type.getModifiers())) &&
                !type.isPrimitive() && !type.isArray() && !type.isEnum();
    }

    public static boolean isAssignable(Type superType, Type subType) {
        return (((superType instanceof ParameterizedType
                && Arrays.stream(((ParameterizedType) superType).getActualTypeArguments())
                .allMatch(arg -> arg instanceof TypeVariable))
                || (superType instanceof GenericArrayType &&
                ((GenericArrayType) superType).getGenericComponentType() instanceof TypeVariable))
                && ClassUtils.getRawType(superType).isAssignableFrom(ClassUtils.getRawType(subType)))
                || (GenericTypeReflector.box(subType) == superType)
                || GenericTypeReflector.isSuperType(superType, subType);
    }

    public static boolean isSuperClass(Class<?> superClass, AnnotatedType subType) {
        return superClass.isAssignableFrom(GenericTypeReflector.erase(subType.getType()));
    }

    public static boolean isSuperClass(AnnotatedType superType, Class<?> subClass) {
        return GenericTypeReflector.erase(superType.getType()).isAssignableFrom(subClass);
    }

    public static boolean isSubPackage(Package pkg, String prefix) {
        String packageName = pkg != null ? pkg.getName() : "";
        return packageName.startsWith(prefix);
    }

    public static String toString(AnnotatedType type) {
        return GenericTypeReflector.toCanonical(type).toString();
    }

    public static String toString(AnnotatedElement element) {
        if (element instanceof Parameter) {
            return ((Parameter) element).getDeclaringExecutable() + "#" + ((Parameter) element).getName();
        }
        if (element instanceof AnnotatedType) {
            return toString((AnnotatedType) element);
        }
        return element.toString();
    }

    /**
     * Checks if an annotation is present either directly on the {@code element}, or as a <b>1st level</b> meta-annotation
     *
     * @param element The element to search the annotation on
     * @param annotation The type of the annotation to search for
     * @return {@code true} if the annotation of type {@code annotation} is found, {@code false} otherwise
     */
    public static boolean hasAnnotation(AnnotatedElement element, Class<? extends Annotation> annotation) {
        return element.isAnnotationPresent(annotation) || Arrays.stream(element.getAnnotations())
                .anyMatch(ann -> ann.annotationType().isAnnotationPresent(annotation));
    }

    /**
     * Checks if an annotation is present either directly on the {@code element}, or recursively as a meta-annotation,
     * at <b>any level</b>
     *
     * @param element The element to search the annotation on
     * @param annotation The type of the annotation to search for
     * @return {@code true} if the annotation of type {@code annotation} is found, {@code false} otherwise
     */
    public static boolean hasMetaAnnotation(AnnotatedElement element, Class<? extends Annotation> annotation) {
        return hasMetaAnnotation(element, annotation, new HashSet<>());
    }

    private static boolean hasMetaAnnotation(AnnotatedElement element, Class<? extends Annotation> annotation, Set<AnnotatedElement> seen) {
        if (seen.contains(element)) {
            return false;
        }
        seen.add(element);
        return element.isAnnotationPresent(annotation) || Arrays.stream(element.getAnnotations())
                .anyMatch(ann -> hasMetaAnnotation(ann.annotationType(), annotation, seen));
    }

    public static <T extends Annotation> Optional<T> findApplicableAnnotation(AnnotatedElement element, Class<T> annotation) {
        if (element.isAnnotationPresent(annotation)) {
            return Optional.of(element.getAnnotation(annotation));
        }
        if (element instanceof Member) {
            Class<?> declaringClass = ((Member) element).getDeclaringClass();
            if (declaringClass.isAnnotationPresent(annotation)){
                return Optional.of(declaringClass.getAnnotation(annotation));
            }
            if (declaringClass.getPackage() != null && declaringClass.getPackage().isAnnotationPresent(annotation)) {
                return Optional.of(declaringClass.getPackage().getAnnotation(annotation));
            }
        }
        return Optional.empty();
    }

    public static List<Method> getAnnotationFields(Class<? extends Annotation> annotation) {
        return Arrays.stream(annotation.getMethods())
                .filter(method -> annotation.equals(method.getDeclaringClass()))
                .collect(Collectors.toList());
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
        return type instanceof AnnotatedArrayType && containsTypeAnnotation(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(), annotation);
    }

    public static Annotation[] getAllAnnotations(Stream<AnnotatedType> types) {
        return types
                .flatMap(type -> Arrays.stream(type.getAnnotations()))
                .distinct()
                .toArray(Annotation[]::new);
    }

    public static <T extends AnnotatedType> T addAnnotations(T type, Annotation... annotations) {
        if (type == null || annotations == null || annotations.length == 0) return type;
        return GenericTypeReflector.updateAnnotations(type, merge(type.getAnnotations(), annotations));
    }

    public static <T extends AnnotatedType> T removeAnnotations(T type, Set<Class<? extends Annotation>> toRemove) {
        if (type.getAnnotations().length == 0 || toRemove.size() == 0) {
            return type;
        }
        Collection<Annotation> keptAnnotations = new ArrayList<>(type.getAnnotations().length);
        for (Annotation annotation : type.getAnnotations()) {
            if (!toRemove.contains(annotation.annotationType())) {
                keptAnnotations.add(annotation);
            }
        }
        return GenericTypeReflector.replaceAnnotations(type, keptAnnotations.toArray(new Annotation[0]));
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
                    throw TypeMappingException.ambiguousType(type.getType());
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
                    throw TypeMappingException.ambiguousType(type.getType());
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
            Class<?> clazz = (Class<?>) type.getType();
            if (clazz.isArray()) {
                return TypeFactory.arrayOf(completeGenerics(GenericTypeReflector.annotate(clazz.getComponentType()), replacement), type.getAnnotations());
            } else {
                if (isMissingTypeParameters(clazz)) {
                    if (replacement == null) {
                        throw TypeMappingException.ambiguousType(clazz);
                    }
                    AnnotatedType[] parameters = new AnnotatedType[clazz.getTypeParameters().length];
                    Arrays.fill(parameters, replacement);
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

    @SuppressWarnings("unchecked")
    public static <T extends AnnotatedType> T transformType(T type, UnaryOperator<T> transformer) {
        if (type instanceof AnnotatedArrayType) {
            return (T) TypeFactory.arrayOf(transformer.apply((T) ((AnnotatedArrayType) type).getAnnotatedGenericComponentType()), type.getAnnotations());
        }
        if (type.getType() instanceof Class) {
            return type;
        }
        if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) type;
            AnnotatedType[] arguments = Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .map(param -> transformer.apply((T) param))
                    .toArray(AnnotatedType[]::new);
            return (T) TypeFactory.parameterizedAnnotatedClass(GenericTypeReflector.erase(type.getType()), type.getAnnotations(), arguments);
        }
        throw new IllegalArgumentException("Can not find the mappable type for: " + type.getType().getTypeName());
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
        Annotation[] mergedAnnotations = getMergedAnnotations(types.toArray(new AnnotatedType[0]));
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
        if (normalizedTypes.stream().anyMatch(type -> isMissingTypeParameters(type.getType()))) {
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
    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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
                || KNOWN_PROXY_CLASS_SEPARATORS.stream().anyMatch(separator -> clazz.getName().contains(separator));
    }

    public static Class<?> forName(String className) throws ClassNotFoundException {
        return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
    }

    public static Object getDefaultValueForType(Class<?> type) {
        return DEFAULT_PRIMITIVE_VALUES.get(type);
    }

    public static boolean isPrimitive(AnnotatedType type) {
        return type.getType().getClass() == Class.class && ((Class<?>) type.getType()).isPrimitive();
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
