package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ObjectTypeMapper extends CachingMapper<GraphQLObjectType, GraphQLInputObjectType> {

    @Override
    public GraphQLObjectType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        //TODO remove the reference to @RelayId here. Instead, check for mapped fields of type GraphQLID
        Optional<Operation> relayId = buildContext.operationRepository.getDomainQueries(javaType).stream()
                .filter(query -> query.getJavaType().isAnnotationPresent(RelayId.class))
                .findFirst();

        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(typeName)
                .description(buildContext.typeMetaDataGenerator.generateTypeDescription(javaType));

        buildContext.operationRepository.getChildQueries(javaType)
                .forEach(childQuery -> typeBuilder.field(operationMapper.toGraphQLOperation(childQuery, buildContext)));

        Set<String> interfaceNames = new HashSet<>();
        if (relayId.isPresent()) {
            typeBuilder.withInterface(operationMapper.node);
            interfaceNames.add(operationMapper.node.getName());
        }
        buildContext.interfaceStrategy.getInterfaces(javaType).forEach(
                inter -> {
                    GraphQLOutputType graphQLInterface = operationMapper.toGraphQLType(inter, abstractTypes, buildContext);
                    typeBuilder.withInterface((GraphQLInterfaceType) graphQLInterface);
                    interfaceNames.add(graphQLInterface.getName());
                });

        if (ClassUtils.isAbstract(javaType)) {
            typeBuilder.field(newFieldDefinition()
                    .name("_type_")
                    .type(Scalars.GraphQLString)
                    .dataFetcher(env -> env.getSource() == null ? null : env.getSource().getClass().getSimpleName())
                    .build());
        }
        GraphQLObjectType type = typeBuilder.build();
        buildContext.typeRepository.registerCovariantTypes(interfaceNames, javaType, type);
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(typeName)
                .description(buildContext.typeMetaDataGenerator.generateInputTypeDescription(javaType));

        buildContext.inputFieldStrategy.getInputFields(javaType).forEach(
                field -> typeBuilder.field(operationMapper.toGraphQLInputField(field, abstractTypes, buildContext)));

        if (ClassUtils.isAbstract(javaType)) {
            typeBuilder.field(newInputObjectField()
                    .name("_type_")
                    .type(Scalars.GraphQLString)
                    .build());
        }
        return typeBuilder.build();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
