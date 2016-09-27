package io.leangen.graphql.generator.mapping;

import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.gentyref8.TypeFactory;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public class MapToListTypeAdapter<K,V> extends AbstractTypeAdapter<Map<K,V>, List<AbstractMap.SimpleEntry<K,V>>> {

    @Override
    public List<AbstractMap.SimpleEntry<K,V>> convertOutput(Map<K,V> original) {
        return original.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<K,V> convertInput(List<AbstractMap.SimpleEntry<K,V>> original) {
        return original.stream().collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType[] mapType = ((AnnotatedParameterizedType) original).getAnnotatedActualTypeArguments();
        Type entryType = TypeFactory.parameterizedClass(AbstractMap.SimpleEntry.class, mapType[0].getType(), mapType[1].getType());
        return GenericTypeReflector.replaceAnnotations(GenericTypeReflector.annotate(TypeFactory.parameterizedClass(List.class, entryType)), original.getAnnotations());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Map.class, type.getType());
    }
}
