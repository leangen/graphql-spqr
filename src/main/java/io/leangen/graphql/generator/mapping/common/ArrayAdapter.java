package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.IntStream;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ArrayAdapter extends AbstractTypeSubstitutingMapper implements OutputConverter {

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType component = ((AnnotatedArrayType) original).getAnnotatedGenericComponentType();
        Class<?> raw = ClassUtils.getRawType(component.getType());
        if (raw.isPrimitive()) {
            component = GenericTypeReflector.annotate(ClassUtils.box(raw), component.getAnnotations());
        }
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), component);
    }

    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return IntStream.range(0, Array.getLength(original))
                .mapToObj(i -> resolutionEnvironment.convertOutput(Array.get(original, i), ((AnnotatedArrayType) type).getAnnotatedGenericComponentType()))
                .toArray();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type instanceof AnnotatedArrayType;
    }
}
