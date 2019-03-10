package io.leangen.graphql.generator.mapping.common;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ArrayAdapter extends AbstractTypeSubstitutingMapper implements DelegatingOutputConverter {

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType component = ((AnnotatedArrayType) original).getAnnotatedGenericComponentType();
        Class<?> raw = ClassUtils.getRawType(component.getType());
        if (raw.isPrimitive()) {
            component = GenericTypeReflector.annotate(GenericTypeReflector.box(raw), component.getAnnotations());
        }
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), component);
    }

    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return IntStream.range(0, Array.getLength(original))
                .mapToObj(i -> resolutionEnvironment.convertOutput(Array.get(original, i), getElementType(type)))
                .toArray();
    }

    @Override
    public List<AnnotatedType> getDerivedTypes(AnnotatedType type) {
        return Collections.singletonList(getElementType(type));
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return !Scalars.isScalar(type.getType()) && type instanceof AnnotatedArrayType;
    }

    private AnnotatedType getElementType(AnnotatedType arrayType) {
        return ((AnnotatedArrayType) arrayType).getAnnotatedGenericComponentType();
    }
}
