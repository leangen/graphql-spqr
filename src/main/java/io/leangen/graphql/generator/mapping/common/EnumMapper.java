package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.JavaDeprecationMappingConfig;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.ReservedStrings;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;

import static graphql.schema.GraphQLEnumType.newEnum;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class EnumMapper extends CachingMapper<GraphQLEnumType, GraphQLEnumType> {

    protected final JavaDeprecationMappingConfig javaDeprecationConfig;

    public EnumMapper(JavaDeprecationMappingConfig javaDeprecationConfig) {
        this.javaDeprecationConfig = javaDeprecationConfig;
    }

    @Override
    public GraphQLEnumType toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLEnumType.Builder enumBuilder = newEnum()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateEnumTypeDescription(javaType, buildContext.messageBundle));
        buildContext.directiveBuilder.buildEnumTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                enumBuilder.withAppliedDirective(env.operationMapper.toGraphQLAppliedDirective(directive, buildContext)));
        addOptions(enumBuilder, javaType, env.operationMapper, buildContext);
        enumBuilder.comparatorRegistry(buildContext.comparatorRegistry(javaType));
        return enumBuilder.build();
    }

    @Override
    public GraphQLEnumType toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        return toGraphQLType(typeName, javaType, env);
    }

    private void addOptions(GraphQLEnumType.Builder enumBuilder, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        MessageBundle messageBundle = buildContext.messageBundle;
        Arrays.stream((Enum[]) ClassUtils.getRawType(javaType.getType()).getEnumConstants())
                .map(enumConst -> (Enum<?>) enumConst)
                .forEach(enumConst -> enumBuilder.value(GraphQLEnumValueDefinition.newEnumValueDefinition()
                        .name(getValueName(enumConst, messageBundle))
                        .value(enumConst)
                        .description(getValueDescription(enumConst, messageBundle))
                        .deprecationReason(getValueDeprecationReason(enumConst, messageBundle))
                        .withAppliedDirectives(getValueDirectives(enumConst, operationMapper, buildContext))
                        .build()));
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

    protected GraphQLAppliedDirective[] getValueDirectives(Enum<?> value, OperationMapper operationMapper, BuildContext buildContext) {
        return buildContext.directiveBuilder.buildEnumValueDirectives(value, buildContext.directiveBuilderParams()).stream()
                .map(directive -> operationMapper.toGraphQLAppliedDirective(directive, buildContext))
                .toArray(GraphQLAppliedDirective[]::new);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isEnum();
    }
}
