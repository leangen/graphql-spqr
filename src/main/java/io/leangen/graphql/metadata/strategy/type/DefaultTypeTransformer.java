package io.leangen.graphql.metadata.strategy.type;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.util.Arrays;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.generator.exceptions.TypeMappingException;

public class DefaultTypeTransformer implements TypeTransformer {
    
    private final boolean replaceUnbounded;
    private final boolean replaceRaw;

    public DefaultTypeTransformer(boolean replaceUnbounded, boolean replaceRaw) {
        this.replaceUnbounded = replaceUnbounded;
        this.replaceRaw = replaceRaw;
    }

    @Override
    public AnnotatedType transform(AnnotatedType annotatedType) throws TypeMappingException {
        annotatedType = stripBounds(annotatedType, replaceUnbounded);
        return completeGenerics(annotatedType, replaceRaw);
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
    private AnnotatedType stripBounds(AnnotatedType type, boolean replaceUnbounded) {
        if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) type;
            AnnotatedType bound = wildcard.getAnnotatedLowerBounds().length > 0
                    ? stripBounds(wildcard.getAnnotatedLowerBounds()[0], replaceUnbounded)
                    : stripBounds(wildcard.getAnnotatedUpperBounds()[0], replaceUnbounded);
            if (bound.getType().equals(Object.class) && !replaceUnbounded) {
                throw new TypeMappingException(type.getType());
            }
            return GenericTypeReflector.updateAnnotations(bound, type.getAnnotations());
        }
        if (type instanceof AnnotatedTypeVariable) {
            AnnotatedType bound = ((AnnotatedTypeVariable) type).getAnnotatedBounds()[0];
            if (bound.getType().equals(Object.class) && !replaceUnbounded) {
                throw new TypeMappingException(type.getType());
            }
            return GenericTypeReflector.updateAnnotations(bound, type.getAnnotations());
        }
        if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) type;
            AnnotatedType[] typeArguments = Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .map(parameterType -> stripBounds(parameterType, replaceUnbounded))
                    .toArray(AnnotatedType[]::new);
            return GenericTypeReflector.replaceParameters(parameterizedType, typeArguments);
        }
        if (type instanceof AnnotatedArrayType) {
            return TypeFactory.arrayOf(stripBounds(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(), replaceUnbounded), type.getAnnotations());
        }
        return type;
    }

    private AnnotatedType completeGenerics(AnnotatedType type, boolean replaceRaw) {
        if (type.getType() instanceof Class) {
            Class clazz = (Class) type.getType();
            if (clazz.isArray()) {
                return TypeFactory.arrayOf(completeGenerics(GenericTypeReflector.annotate(clazz.getComponentType()), replaceRaw), type.getAnnotations());
            } else {
                if (GenericTypeReflector.isMissingTypeParameters(clazz)) {
                    if (!replaceRaw) {
                        throw new TypeMappingException(clazz);
                    }
                    AnnotatedType[] parameters = new AnnotatedType[clazz.getTypeParameters().length];
                    for (int i = 0; i < parameters.length; i++) {
                        parameters[i] = GenericTypeReflector.annotate(Object.class);
                    }
                    return TypeFactory.parameterizedAnnotatedClass(clazz, type.getAnnotations(), parameters);
                }
            }
        }
        else if (type instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) type;
            AnnotatedType[] parameters = Arrays.stream(parameterizedType.getAnnotatedActualTypeArguments())
                    .map(parameterType -> completeGenerics(parameterType, replaceRaw))
                    .toArray(AnnotatedType[]::new);
            return GenericTypeReflector.replaceParameters(parameterizedType, parameters);
        }
        else if (type instanceof AnnotatedArrayType) {
            AnnotatedType componentType = completeGenerics(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(), replaceRaw);
            return TypeFactory.arrayOf(componentType, type.getAnnotations());
        }
        else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
            //can only happen if bounds haven't been stripped (via stripBounds) prior to invoking this method
            //which should never be the case (and this method is private so can't be misused from outside)
            throw new TypeMappingException(type.getType().getTypeName() + " can not validated");
        }
        return type;
    }
}
