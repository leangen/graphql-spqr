package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.query.ExecutionContext;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public class MapToListTypeAdapter<K,V> extends AbstractTypeAdapter<Map<K,V>, List<AbstractMap.SimpleEntry<K,V>>> {

    @Override
    public List<AbstractMap.SimpleEntry<K,V>> convertOutput(Map<K, V> original, AnnotatedType type, ExecutionContext executionContext) {
        return original.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<K,V> convertInput(List<AbstractMap.SimpleEntry<K, V>> original, AnnotatedType type, ExecutionContext executionContext) {
        return original.stream().collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType[] mapType = ((AnnotatedParameterizedType) original).getAnnotatedActualTypeArguments();
        Type entryType = TypeFactory.parameterizedClass(AbstractMap.SimpleEntry.class, mapType[0].getType(), mapType[1].getType());
        return GenericTypeReflector.annotate(TypeFactory.parameterizedClass(List.class, entryType), original.getAnnotations());
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return new GraphQLList(
                mapEntry(
                        queryGenerator.toGraphQLType(ClassUtils.getTypeArguments(javaType)[0], buildContext),
                        queryGenerator.toGraphQLType(ClassUtils.getTypeArguments(javaType)[1], buildContext)));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return new GraphQLList(
                mapEntry(
                        queryGenerator.toGraphQLInputType(ClassUtils.getTypeArguments(javaType)[0], buildContext),
                        queryGenerator.toGraphQLInputType(ClassUtils.getTypeArguments(javaType)[1], buildContext)));
    }

    private GraphQLOutputType mapEntry(GraphQLOutputType keyType, GraphQLOutputType valueType) {
        return newObject()
                .name("mapEntry_" + keyType.getName() + "_" + valueType.getName())
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

    private GraphQLInputType mapEntry(GraphQLInputType keyType, GraphQLInputType valueType) {
        return newInputObject()
                .name("mapEntry_" + keyType.getName() + "_" + valueType.getName() + "_input")
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
}
