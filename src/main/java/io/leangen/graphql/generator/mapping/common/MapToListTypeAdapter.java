package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLModifiedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.generator.mapping.DelegatingOutputConverter;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static io.leangen.graphql.util.GraphQLUtils.name;

/**
 * As maps are dynamic structures with no equivalent in GraphQL, they require special treatment.
 * This adapter turns a map into a list of key-value pairs (instances of {@link MapToListTypeAdapter.MapEntry}).
 */
public class MapToListTypeAdapter extends AbstractTypeAdapter<Map<?, ?>, List<MapToListTypeAdapter.MapEntry<?, ?>>>
        implements DelegatingOutputConverter<Map<?, ?>, List<MapToListTypeAdapter.MapEntry<?, ?>>> {

    private final MapOutputConverter converter;

    public MapToListTypeAdapter() {
        this.converter = new MapOutputConverter();
    }

    private MapToListTypeAdapter(MapOutputConverter converter) {
        this.converter = converter;
    }

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
        Type entryType = TypeFactory.parameterizedClass(MapToListTypeAdapter.MapEntry.class, keyType.getType(), valueType.getType());
        return GenericTypeReflector.annotate(TypeFactory.parameterizedClass(List.class, entryType), original.getAnnotations());
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return new GraphQLList(new GraphQLNonNull(
                mapEntry(
                        //MapEntry fields are artificial - no Java element is backing them
                        env.forElement(null).toGraphQLType(getElementType(javaType, 0)),
                        env.forElement(null).toGraphQLType(getElementType(javaType, 1)), env.buildContext)));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return new GraphQLList(new GraphQLNonNull(
                mapEntry(
                        //MapEntry fields are artificial - no Java element is backing them
                        env.forElement(null).toGraphQLInputType(getElementType(javaType, 0)),
                        env.forElement(null).toGraphQLInputType(getElementType(javaType, 1)), env.buildContext)));
    }

    @Override
    public List<AnnotatedType> getDerivedTypes(AnnotatedType mapType) {
        return converter.getDerivedTypes(mapType);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperClass(Map.class, type) && !type.isAnnotationPresent(GraphQLScalar.class);
    }

    private GraphQLOutputType mapEntry(GraphQLOutputType keyType, GraphQLOutputType valueType, BuildContext buildContext) {
        String typeName = "mapEntry_" + getTypeName(keyType) + "_" + getTypeName(valueType);
        if (buildContext.typeCache.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.typeCache.register(typeName);

        return newObject()
                .name(typeName)
                .description("Map entry")
                .field(newFieldDefinition()
                        .name("key")
                        .description("Map key")
                        .type(keyType)
                        .build())
                .field(newFieldDefinition()
                        .name("value")
                        .description("Map value")
                        .type(valueType)
                        .build())
                .build();
    }

    private GraphQLInputType mapEntry(GraphQLInputType keyType, GraphQLInputType valueType, BuildContext buildContext) {
        String typeName = "mapEntry_" + getTypeName(keyType) + "_" + getTypeName(valueType) + "_input";
        if (buildContext.typeCache.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.typeCache.register(typeName);

        return newInputObject()
                .name(typeName)
                .description("Map entry input")
                .field(newInputObjectField()
                        .name("key")
                        .description("Map key input")
                        .type(keyType)
                        .build())
                .field(newInputObjectField()
                        .name("value")
                        .description("Map value input")
                        .type(valueType)
                        .build())
                .build();
    }

    private AnnotatedType getElementType(AnnotatedType javaType, int index) {
        return GenericTypeReflector.getTypeParameter(javaType, Map.class.getTypeParameters()[index]);
    }

    private String getTypeName(GraphQLType type) {
        if (type instanceof GraphQLModifiedType) {
            StringBuilder name = new StringBuilder();
            while (type instanceof GraphQLModifiedType) {
                if (type instanceof GraphQLList) {
                    name.append("list_");
                } else {
                    name.append("__");
                }
                type = ((GraphQLModifiedType) type).getWrappedType();
            }
            return name.append(name(type)).toString();
        } else {
            return name(type);
        }
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
    public static class MapEntry<K, V> {
        private K key;
        private V value;

        public MapEntry() {
        }

        public MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public void setKey(K key) {
            this.key = key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }
}
