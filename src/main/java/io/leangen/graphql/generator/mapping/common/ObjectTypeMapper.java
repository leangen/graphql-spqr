package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.types.MappedGraphQLObjectType;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ObjectTypeMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

    @Override
    public GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType));

        List<GraphQLFieldDefinition> fields = getFields(javaType, buildContext, operationMapper);
        fields.forEach(typeBuilder::field);

        List<GraphQLOutputType> interfaces = getInterfaces(javaType, fields, buildContext, operationMapper);
        interfaces.forEach(inter -> {
            if (inter instanceof GraphQLInterfaceType) {
                typeBuilder.withInterface((GraphQLInterfaceType) inter);
            } else {
                typeBuilder.withInterface((GraphQLTypeReference) inter);
            }
        });

        GraphQLObjectType type = new MappedGraphQLObjectType(typeBuilder.build(), javaType);
        interfaces.forEach(inter -> buildContext.typeRepository.registerCovariantType(inter.getName(), javaType, type));
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType));

        buildContext.inputFieldStrategy.getInputFields(javaType, buildContext.inclusionStrategy, buildContext.typeTransformer).forEach(
                field -> typeBuilder.field(operationMapper.toGraphQLInputField(field, buildContext)));

        if (ClassUtils.isAbstract(javaType)) {
            createInputDisambiguatorField(javaType, buildContext).ifPresent(typeBuilder::field);
        }
        return typeBuilder.build();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    protected List<GraphQLFieldDefinition> getFields(AnnotatedType javaType, BuildContext buildContext, OperationMapper operationMapper) {
        List<GraphQLFieldDefinition> fields = buildContext.operationRepository.getChildQueries(javaType).stream()
                .map(childQuery -> operationMapper.toGraphQLField(childQuery, buildContext))
                .collect(Collectors.toList());
        return sortFields(fields, buildContext.typeInfoGenerator.getFieldOrder(javaType));
    }

    @SuppressWarnings("WeakerAccess")
    protected List<GraphQLOutputType> getInterfaces(AnnotatedType javaType,
                                                    List<GraphQLFieldDefinition> fields, BuildContext buildContext, OperationMapper operationMapper) {

        List<GraphQLOutputType> interfaces = new ArrayList<>();
        if (buildContext.relayMappingConfig.inferNodeInterface && fields.stream().anyMatch(GraphQLUtils::isRelayId)) {
            interfaces.add(buildContext.node);
        }
        buildContext.interfaceStrategy.getInterfaces(javaType).forEach(
                inter -> interfaces.add(operationMapper.toGraphQLType(inter, buildContext)));

        return interfaces;
    }

    private static List<GraphQLFieldDefinition> sortFields(List<GraphQLFieldDefinition> fields, String[] specifiedFieldOrder) {
        Map<String, GraphQLFieldDefinition> fieldMap = new TreeMap<>();
        for (GraphQLFieldDefinition field : fields) {
            fieldMap.put(field.getName(), field);
        }
        List<GraphQLFieldDefinition> result = new ArrayList<>();
        for (String name : specifiedFieldOrder) {
            if (fieldMap.containsKey(name)) {
                result.add(fieldMap.remove(name));
            }
        }
        result.addAll(fieldMap.values());
        return result;
    }

    @SuppressWarnings("WeakerAccess")
    protected Optional<GraphQLInputObjectField> createInputDisambiguatorField(AnnotatedType javaType, BuildContext buildContext) {
        Class<?> raw = ClassUtils.getRawType(javaType.getType());
        String typeName = buildContext.typeInfoGenerator.generateTypeName(GenericTypeReflector.annotate(raw)) + "TypeDisambiguator";
        GraphQLInputType fieldType = null;
        if (buildContext.typeCache.contains(typeName)) {
            fieldType = new GraphQLTypeReference(typeName);
        } else {
            List<AnnotatedType> impls = buildContext.abstractInputHandler.findConcreteSubTypes(raw, buildContext).stream()
                    .map(GenericTypeReflector::annotate)
                    .collect(Collectors.toList());
            if (!impls.isEmpty()) {
                buildContext.typeCache.register(typeName);
                GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum()
                        .name(typeName)
                        .description("Input type disambiguator");
                impls.stream()
                        .map(buildContext.typeInfoGenerator::generateTypeName)
                        .forEach(builder::value);
                fieldType = builder.build();
            }
        }
        return Optional.ofNullable(fieldType).map(type -> newInputObjectField()
                .name(ValueMapper.TYPE_METADATA_FIELD_NAME)
                .type(type)
                .build());
    }
}
