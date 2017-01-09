package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.metadata.strategy.input.InputDeserializer;
import io.leangen.graphql.query.ExecutionContext;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class StreamToCollectionTypeAdapter extends AbstractTypeAdapter<Stream<?>, List<?>> {

    @Override
    public List<?> convertOutput(Stream<?> original, AnnotatedType type, InputDeserializer inputDeserializer, ExecutionContext executionContext) {
        AnnotatedType inner = ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()[0];
        return original
                .map(item -> executionContext.convertOutput(item, inner, inputDeserializer))
                .collect(Collectors.toList());
    }

    @Override
    public Stream<?> convertInput(List<?> substitute, AnnotatedType type, ExecutionContext executionContext) {
        AnnotatedType inner = ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()[0];
        return substitute.stream().map(item -> executionContext.convertInput(item, inner));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType[] streamType = ((AnnotatedParameterizedType) original).getAnnotatedActualTypeArguments();
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), streamType);
    }
}
