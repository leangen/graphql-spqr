package io.leangen.graphql.generator.mapping.common;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * As maps are dynamic structures with no equivalent in GraphQL, they require special treatment.
 * This adapter turns a map into a list of key-value pairs (instances of {@link MapToListTypeAdapter.MapEntry}).
 */
public class MapToListTypeAdapter extends AbstractTypeAdapter<Map<?, ?>, List<MapToListTypeAdapter.MapEntry<?, ?>>>
        implements DelegatingOutputConverter<Map<?, ?>, List<MapToListTypeAdapter.MapEntry<?, ?>>> {

    private final MapOutputConverter converter = new MapOutputConverter();

    @Override
    public List<MapToListTypeAdapter.MapEntry<?, ?>> convertOutput(Map<?, ?> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return converter.convertOutput(original, type, resolutionEnvironment).entrySet().stream()
                .map(entry -> new MapToListTypeAdapter.MapEntry<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<?, ?> convertInput(List<MapEntry<?, ?>> original, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        Map<Object, Object> initial = ClassUtils.isSuperClass(type, HashMap.class) ? new HashMap<>() : ClassUtils.instance(type);
        return original.stream().collect(toMap(MapEntry::getKey, MapEntry::getValue, initial));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType keyType = getElementType(original, 0);
        AnnotatedType valueType = getElementType(original, 1);
        AnnotatedType entryType = TypeFactory.parameterizedAnnotatedClass(MapEntry.class, new Annotation[0], keyType, valueType);
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), entryType);
    }

    @Override
    public List<AnnotatedType> getDerivedTypes(AnnotatedType mapType) {
        return converter.getDerivedTypes(mapType);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperClass(Map.class, type) && !type.isAnnotationPresent(GraphQLScalar.class);
    }

    private AnnotatedType getElementType(AnnotatedType javaType, int index) {
        return GenericTypeReflector.getTypeParameter(javaType, Map.class.getTypeParameters()[index]);
    }

    private static <T, K, U> Collector<T, ?, Map<K,U>> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper,
            Map<K,U> initial) {
        return Collectors.toMap(keyMapper, valueMapper,
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                },
                () -> initial);
    }

    @SuppressWarnings("WeakerAccess")
    @io.leangen.graphql.annotations.types.GraphQLType(description = "Map entry input")
    public static class MapEntry<K, V> {
        private K key;
        private V value;

        public MapEntry() {
        }

        public MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @GraphQLQuery(description = "Map key")
        public K getKey() {
            return key;
        }

        public void setKey(K key) {
            this.key = key;
        }

        @GraphQLQuery(description = "Map value")
        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }
}
