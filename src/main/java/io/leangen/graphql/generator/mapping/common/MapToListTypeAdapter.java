package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.generator.mapping.strategy.ScalarMappingStrategy;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * As maps are dynamic structures with no equivalent in GraphQL, they require special treatment.
 * This adapter turns a map into a list of key-value pairs (instances of {@link MapToListTypeAdapter.MapEntry}).
 */
public class MapToListTypeAdapter<K,V> extends AbstractTypeAdapter<Map<K,V>, List<MapToListTypeAdapter.MapEntry<K,V>>> {

    private final ScalarMappingStrategy scalarStrategy;

    public MapToListTypeAdapter(ScalarMappingStrategy scalarStrategy) {
        this.scalarStrategy = scalarStrategy;
    }
    
    @Override
    public List<MapToListTypeAdapter.MapEntry<K,V>> convertOutput(Map<K, V> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.entrySet().stream()
                .map(entry -> new MapToListTypeAdapter.MapEntry<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<K,V> convertInput(List<MapToListTypeAdapter.MapEntry<K, V>> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return original.stream().collect(Collectors.toMap(MapToListTypeAdapter.MapEntry::getKey, MapToListTypeAdapter.MapEntry::getValue));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType keyType = getElementType(original, 0);
        AnnotatedType valueType = getElementType(original, 1);
        Type entryType = TypeFactory.parameterizedClass(MapToListTypeAdapter.MapEntry.class, keyType.getType(), valueType.getType());
        return GenericTypeReflector.annotate(TypeFactory.parameterizedClass(List.class, entryType), original.getAnnotations());
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return new GraphQLList(
                mapEntry(
                        operationMapper.toGraphQLType(getElementType(javaType, 0), abstractTypes, buildContext),
                        operationMapper.toGraphQLType(getElementType(javaType, 1), abstractTypes, buildContext), buildContext));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return new GraphQLList(
                mapEntry(
                        operationMapper.toGraphQLInputType(getElementType(javaType, 0), abstractTypes, buildContext),
                        operationMapper.toGraphQLInputType(getElementType(javaType, 1), abstractTypes, buildContext), buildContext));
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return super.supports(type) && !scalarStrategy.supports(type);
    }

    private GraphQLOutputType mapEntry(GraphQLOutputType keyType, GraphQLOutputType valueType, BuildContext buildContext) {
        String typeName = "mapEntry_" + keyType.getName() + "_" + valueType.getName();
        if (buildContext.knownTypes.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.knownTypes.add(typeName);
        
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
        String typeName = "mapEntry_" + keyType.getName() + "_" + valueType.getName() + "_input";
        if (buildContext.knownInputTypes.contains(typeName)) {
            return new GraphQLTypeReference(typeName);
        }
        buildContext.knownInputTypes.add(typeName);
        
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
