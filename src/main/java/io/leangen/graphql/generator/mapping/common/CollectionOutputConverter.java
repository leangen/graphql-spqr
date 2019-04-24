package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.OutputConverter;

/**
 * Only used to trigger the conversion on the components of collections, or keys/values of maps
 * @author Bojan Tomic (kaqqao)
 */
public class CollectionOutputConverter implements OutputConverter {

    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        if (GenericTypeReflector.isSuperType(Collection.class, type.getType())) {
            return processCollection((Collection<?>) original, type, resolutionEnvironment);
        }
        return processMap((Map<?, ?>) original, type, resolutionEnvironment);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Collection.class, type.getType())
                || GenericTypeReflector.isSuperType(Map.class, type.getType());
    }

    private List<?> processCollection(Collection<?> collection, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        AnnotatedParameterizedType collectionType =
                (AnnotatedParameterizedType) GenericTypeReflector.getExactSuperType(type, Collection.class);

        return collection.stream()
                .map(e -> resolutionEnvironment.convertOutput(e, collectionType.getAnnotatedActualTypeArguments()[0]))
                .collect(Collectors.toList());
    }

    private Map<?, ?> processMap(Map<?, ?> map, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        AnnotatedParameterizedType mapType =
                (AnnotatedParameterizedType) GenericTypeReflector.getExactSuperType(type, Map.class);

        Map<?, ?> processed = new LinkedHashMap<>();
        map.forEach((k, v) -> processed.put(
                resolutionEnvironment.convertOutput(k, mapType.getAnnotatedActualTypeArguments()[0]),
                resolutionEnvironment.convertOutput(v, mapType.getAnnotatedActualTypeArguments()[1])));
        return processed;
    }
}
