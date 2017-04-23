package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.GraphQLUtils;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ObjectTypeMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

    @Override
    public GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType));

        List<GraphQLFieldDefinition> fields = getFields(javaType, buildContext, operationMapper);
        fields.forEach(typeBuilder::field);

        List<GraphQLInterfaceType> interfaces = getInterfaces(javaType, abstractTypes, fields, buildContext, operationMapper);
        interfaces.forEach(typeBuilder::withInterface);
        
        GraphQLObjectType type = typeBuilder.build();
        interfaces.forEach(inter -> buildContext.typeRepository.registerCovariantTypes(inter.getName(), javaType, type));
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateInputTypeDescription(javaType));

        buildContext.inputFieldStrategy.getInputFields(javaType).forEach(
                field -> typeBuilder.field(operationMapper.toGraphQLInputField(field, abstractTypes, buildContext)));

        if (ClassUtils.isAbstract(javaType)) {
            typeBuilder.field(newInputObjectField()
                    .name(ValueMapper.TYPE_METADATA_FIELD_NAME)
                    .type(Scalars.GraphQLString)
                    .build());
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
                .map(childQuery -> operationMapper.toGraphQLOperation(childQuery, buildContext))
                .collect(Collectors.toList());
        if (ClassUtils.isAbstract(javaType) || !buildContext.interfaceStrategy.getInterfaces(javaType).isEmpty()) {
            fields.add(newFieldDefinition()
                    .name(ValueMapper.TYPE_METADATA_FIELD_NAME)
                    .type(Scalars.GraphQLString)
                    .dataFetcher(env -> env.getSource() == null ? null : env.getSource().getClass().getSimpleName())
                    .build());
        }
        return fields;
    }
    
    @SuppressWarnings("WeakerAccess")
    protected List<GraphQLInterfaceType> getInterfaces(AnnotatedType javaType, Set<Type> abstractTypes,
                                                       List<GraphQLFieldDefinition> fields, BuildContext buildContext, OperationMapper operationMapper) {

        List<GraphQLInterfaceType> interfaces = new ArrayList<>();
        if (fields.stream().anyMatch(GraphQLUtils::isRelayId)) {
            interfaces.add(operationMapper.node);
        }
        buildContext.interfaceStrategy.getInterfaces(javaType).forEach(
                inter -> interfaces.add((GraphQLInterfaceType) operationMapper.toGraphQLType(inter, abstractTypes, buildContext)));
        
        return interfaces;
    }
}
