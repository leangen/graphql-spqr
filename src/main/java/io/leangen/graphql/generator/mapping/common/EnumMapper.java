package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.JavaDeprecationMappingConfig;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.ReservedStrings;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class EnumMapper extends CachingMapper<GraphQLEnumType, GraphQLEnumType> {

    private final JavaDeprecationMappingConfig javaDeprecationConfig;

    public EnumMapper(JavaDeprecationMappingConfig javaDeprecationConfig) {
        this.javaDeprecationConfig = javaDeprecationConfig;
    }

    @Override
    public GraphQLEnumType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));
        addOptions(enumBuilder, javaType, buildContext.typeInfoGenerator, buildContext.messageBundle);
        return enumBuilder.build();
    }

    @Override
    public GraphQLEnumType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType, buildContext.messageBundle));
        addOptions(enumBuilder, javaType, buildContext.typeInfoGenerator, buildContext.messageBundle);
        return enumBuilder.build();
    }

    private void addOptions(GraphQLEnumType.Builder enumBuilder, AnnotatedType javaType, TypeInfoGenerator infoGenerator, MessageBundle messageBundle) {
        sortEnumValues((Enum[]) ClassUtils.getRawType(javaType.getType()).getEnumConstants(), infoGenerator.getFieldOrder(javaType, messageBundle), messageBundle).stream()
                .map(enumConst -> (Enum<?>) enumConst)
                .forEach(enumConst -> enumBuilder.value(
                        getValueName(enumConst, messageBundle), enumConst, getValueDescription(enumConst, messageBundle), getValueDeprecationReason(enumConst, messageBundle)));
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueName(Enum<?> value, MessageBundle messageBundle) {
        GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(GraphQLEnumValue.class);
        return annotation != null && !annotation.name().isEmpty() ? messageBundle.interpolate(annotation.name()) : value.name();
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueDescription(Enum<?> value, MessageBundle messageBundle) {
        GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(GraphQLEnumValue.class);
        return annotation != null ? messageBundle.interpolate(annotation.description()) : null;
    }

    @SuppressWarnings("WeakerAccess")
    protected String getValueDeprecationReason(Enum<?> value, MessageBundle messageBundle) {
        GraphQLEnumValue annotation = ClassUtils.getEnumConstantField(value).getAnnotation(GraphQLEnumValue.class);
        if (annotation != null) {
            return ReservedStrings.decode(messageBundle.interpolate(annotation.deprecationReason()));
        }
        Deprecated deprecated = ClassUtils.getEnumConstantField(value).getAnnotation(Deprecated.class);
        return javaDeprecationConfig.enabled && deprecated != null ? javaDeprecationConfig.deprecationReason : null;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isEnum();
    }

    private List<Enum> sortEnumValues(Enum[] values, String[] order, MessageBundle messageBundle) {
        Map<String, Enum> fieldMap = new TreeMap<>();
        for (Enum value : values) {
            fieldMap.put(getValueName(value, messageBundle), value);
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
