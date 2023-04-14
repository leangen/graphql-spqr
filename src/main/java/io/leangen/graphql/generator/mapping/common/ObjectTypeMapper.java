package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.metadata.TypeDiscriminatorField;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ObjectTypeMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

    @Override
    public GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType, buildContext.messageBundle));

        List<GraphQLFieldDefinition> fields = getFields(typeName, javaType, env);
        fields.forEach(typeBuilder::field);

        List<GraphQLNamedOutputType> interfaces = getInterfaces(javaType, fields, env);
        interfaces.forEach(inter -> {
            if (inter instanceof GraphQLInterfaceType) {
                typeBuilder.withInterface((GraphQLInterfaceType) inter);
            } else {
                typeBuilder.withInterface((GraphQLTypeReference) inter);
            }
        });

        buildContext.directiveBuilder.buildObjectTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                typeBuilder.withAppliedDirective(env.operationMapper.toGraphQLAppliedDirective(directive, buildContext)));
        typeBuilder.comparatorRegistry(buildContext.comparatorRegistry(javaType));

        GraphQLObjectType type = typeBuilder.build();
        interfaces.forEach(inter -> buildContext.typeRegistry.registerCovariantType(inter.getName(), javaType, type));
        buildContext.typeRegistry.registerMapping(type.getName(), javaType);
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType, buildContext.messageBundle));

        InputFieldBuilderParams params = InputFieldBuilderParams.builder()
                .withType(javaType)
                .withEnvironment(buildContext.globalEnvironment)
                .withConcreteSubTypes(buildContext.abstractInputHandler.findConcreteSubTypes(ClassUtils.getRawType(javaType.getType()), buildContext))
                .build();
        buildContext.inputFieldBuilder.getInputFields(params).forEach(field -> typeBuilder.field(env.operationMapper.toGraphQLInputField(field, buildContext)));
        if (ClassUtils.isAbstract(javaType)) {
            getTypeDiscriminatorField(params, buildContext).ifPresent(typeBuilder::field);
        }

        buildContext.directiveBuilder.buildInputObjectTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                typeBuilder.withAppliedDirective(env.operationMapper.toGraphQLAppliedDirective(directive, buildContext)));
        typeBuilder.comparatorRegistry(buildContext.comparatorRegistry(javaType));
        GraphQLInputObjectType type = typeBuilder.build();
        buildContext.typeRegistry.registerMapping(type.getName(), javaType);
        return type;
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    protected List<GraphQLFieldDefinition> getFields(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        return env.buildContext.operationRegistry.getChildQueries(javaType).stream()
                .map(childQuery -> env.operationMapper.toGraphQLField(typeName, childQuery, env.buildContext))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("WeakerAccess")
    protected List<GraphQLNamedOutputType> getInterfaces(AnnotatedType javaType, List<GraphQLFieldDefinition> fields, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        List<GraphQLNamedOutputType> interfaces = new ArrayList<>();
        if (buildContext.relayMappingConfig.inferNodeInterface && fields.stream().anyMatch(GraphQLUtils::isRelayId)) {
            interfaces.add(buildContext.node);
        }
        buildContext.interfaceStrategy.getInterfaces(javaType).forEach(
                inter -> interfaces.add((GraphQLNamedOutputType) env.operationMapper.toGraphQLType(inter, env)));

        return interfaces;
    }

    @SuppressWarnings("WeakerAccess")
    protected Optional<GraphQLInputObjectField> getTypeDiscriminatorField(InputFieldBuilderParams params, BuildContext buildContext) {
        return Optional.ofNullable(buildContext.inputFieldBuilder.getTypeDiscriminatorField(params))
                .map(discriminator -> newInputObjectField()
                        .name(discriminator.getName())
                        .description(discriminator.getDescription())
                        .type(getDiscriminatorFieldType(discriminator, params.getType(), buildContext))
                        .build());
    }

    private GraphQLInputType getDiscriminatorFieldType(TypeDiscriminatorField discriminator, AnnotatedType type, BuildContext buildContext) {
        Class<?> raw = ClassUtils.getRawType(type.getType());
        //Generate the name for the raw type only, as it should stay the same regardless of generics
        String typeName = buildContext.typeInfoGenerator.generateTypeName(GenericTypeReflector.annotate(raw), buildContext.messageBundle) + "TypeDisambiguator";
        if (buildContext.typeCache.contains(typeName)) {
            return GraphQLTypeReference.typeRef(typeName);
        } else {
            buildContext.typeCache.register(typeName);
            GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum()
                    .name(typeName)
                    .description("Input type discriminator");
            Arrays.stream(discriminator.getValues())
                    .forEach(builder::value);
            return builder.build();
        }
    }
}
