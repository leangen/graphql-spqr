package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.util.List;

import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.generator.mapping.AbstractTypeSubstitutingMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ArrayMapper<S> extends AbstractTypeSubstitutingMapper<S[]> {
    
    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), 
                ((AnnotatedArrayType) original).getAnnotatedGenericComponentType());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type instanceof AnnotatedArrayType;
    }
}
