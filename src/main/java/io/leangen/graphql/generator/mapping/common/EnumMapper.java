package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class EnumMapper extends CachingMapper<GraphQLEnumType, GraphQLEnumType> {

    private final boolean respectJavaDeprecation;

    public EnumMapper(boolean respectJavaDeprecation) {
        this.respectJavaDeprecation = respectJavaDeprecation;
    }

    @Override
    public GraphQLEnumType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType));
        addOptions(enumBuilder, javaType, buildContext.typeInfoGenerator);
        return enumBuilder.build();
    }

    @Override
    public GraphQLEnumType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType));
        addOptions(enumBuilder, javaType, buildContext.typeInfoGenerator);
        return enumBuilder.build();
    }

    private void addOptions(GraphQLEnumType.Builder enumBuilder, AnnotatedType javaType, TypeInfoGenerator infoGenerator) {
        sortEnumValues((Enum[]) ClassUtils.getRawType(javaType.getType()).getEnumConstants(), infoGenerator.getFieldOrder(javaType)).stream()
                .map(enumConst -> (Enum<?>) enumConst)
                .forEach(enumConst -> enumBuilder.value(
                        getValueName(enumConst), enumConst, getValueDescription(enumConst), getValueDeprecationReason(enumConst)));
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueName(Enum<?> value) {
        GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(GraphQLEnumValue.class);
        return annotation != null && !annotation.name().isEmpty() ? annotation.name() : value.name();
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueDescription(Enum<?> value) {
        GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(GraphQLEnumValue.class);
        return annotation != null ? annotation.description() : null;
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueDeprecationReason(Enum<?> value) {
        GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(GraphQLEnumValue.class);
        if (annotation != null) {
            return Utils.decodeNullable(annotation.deprecationReason());
        }
        Deprecated deprecated = ClassUtils.getEnumConstantField(value).getAnnotation(Deprecated.class);
        return respectJavaDeprecation && deprecated != null ? "" : null;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isEnum();
    }

    @Override
    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return buildContext.typeInfoGenerator.generateTypeName(type);
    }

    @Override
    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return buildContext.typeInfoGenerator.generateInputTypeName(type);
    }

    private List<Enum> sortEnumValues(Enum[] values, String[] order) {
        Map<String, Enum> fieldMap = new TreeMap<>();
        for (Enum value : values) {
            fieldMap.put(getValueName(value), value);
        }
        List<Enum> result = new ArrayList<>();
        for (String name : order) {
            if (fieldMap.containsKey(name)) {
                result.add(fieldMap.remove(name));
            }
        }
        result.addAll(fieldMap.values());
        return result;
    }
}
