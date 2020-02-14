package io.leangen.graphql.generator.mapping.common;

import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnumMapToObjectTypeAdapter<E extends Enum<E>, V> extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> implements InputConverter<EnumMap<E, V>, Map<String, V>> {

    private final EnumMapper enumMapper;

    public EnumMapToObjectTypeAdapter(EnumMapper enumMapper) {
        this.enumMapper = enumMapper;
    }


    @Override
    protected GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));

        Enum<E>[] keys = ClassUtils.<E>getRawType(getElementType(javaType, 0).getType()).getEnumConstants();
        Arrays.stream(keys).forEach(enumValue -> {
            String fieldName = enumMapper.getValueName(enumValue, buildContext.messageBundle);
            TypedElement element = new TypedElement(getElementType(javaType, 1), ClassUtils.getEnumConstantField(enumValue));
            buildContext.codeRegistry.dataFetcher(FieldCoordinates.coordinates(typeName, fieldName), (DataFetcher) e -> ((Map)e.getSource()).get(enumValue));
            builder.field(GraphQLFieldDefinition.newFieldDefinition()
                    .name(fieldName)
                    .description(enumMapper.getValueDescription(enumValue, buildContext.messageBundle))
                    .deprecate(enumMapper.getValueDeprecationReason(enumValue, buildContext.messageBundle))
                    .type(env.forElement(element).toGraphQLType(element.getJavaType()))
                    .build());
        });
        return builder.build();
    }

    @Override
    protected GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType, buildContext.messageBundle));

        @SuppressWarnings("rawtypes")
        Enum[] keys = (Enum[]) ClassUtils.getRawType(getElementType(javaType, 0).getType()).getEnumConstants();
        Arrays.stream(keys).forEach(enumValue -> {
            TypedElement element = new TypedElement(getElementType(javaType, 1), ClassUtils.getEnumConstantField(enumValue));
            builder.field(GraphQLInputObjectField.newInputObjectField()
                    .name(enumMapper.getValueName(enumValue, buildContext.messageBundle))
                    .description(enumMapper.getValueDescription(enumValue, buildContext.messageBundle))
                    .type(env.forElement(element).toGraphQLInputType(element.getJavaType()))
                    .build());
        });
        return builder.build();
    }

    @Override
    public EnumMap<E, V> convertInput(Map<String, V> substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        Map<String, E> values = Arrays.stream((ClassUtils.<E>getRawType(getElementType(type, 0).getType()).getEnumConstants()))
                .collect(Collectors.toMap(e -> (enumMapper.getValueName(e, environment.messageBundle)), Function.identity()));
        Map<E, V> m = substitute.entrySet().stream().collect(Collectors.toMap(e -> values.get(e.getKey()), Map.Entry::getValue));
        return new EnumMap<>(m);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperClass(EnumMap.class, type);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return supports(type);
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        AnnotatedType keyType = getElementType(original, 0);
        AnnotatedType valueType = getElementType(original, 1);
        return TypeFactory.parameterizedAnnotatedClass(Map.class, original.getAnnotations(),  GenericTypeReflector.annotate(String.class, keyType.getAnnotations()), valueType);
    }

    private AnnotatedType getElementType(AnnotatedType javaType, int index) {
        return GenericTypeReflector.getTypeParameter(javaType, Map.class.getTypeParameters()[index]);
    }
}
