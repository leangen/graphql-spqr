package io.leangen.graphql.generator.mapping.common;

import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

import static graphql.schema.GraphQLInputObjectType.newInputObject;

public class AnnotationMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

    @Override
    protected GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        throw new UnsupportedOperationException("Annotation type used as output");
    }

    @Override
    protected GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType, buildContext.messageBundle));

        InputFieldBuilderParams params = InputFieldBuilderParams.builder()
                .withType(javaType)
                .withEnvironment(buildContext.globalEnvironment)
                .build();
        buildContext.inputFieldBuilder.getInputFields(params).forEach(inputField -> {
            GraphQLInputObjectField field = env.operationMapper.toGraphQLInputField(inputField, buildContext);
            typeBuilder.field(field);
            buildContext.typeRegistry.registerMapping(FieldCoordinates.coordinates(typeName, field.getName()), inputField);
        });

        return typeBuilder.build();
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.getRawType(type.getType()).isAnnotation();
    }
}
