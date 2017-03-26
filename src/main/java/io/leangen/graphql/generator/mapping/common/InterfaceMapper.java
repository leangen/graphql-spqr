package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.generator.types.MappedGraphQLInterfaceType;

import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InterfaceMapper extends CachingMapper<GraphQLInterfaceType, GraphQLInputObjectType> {

    private final InterfaceMappingStrategy interfaceStrategy;
    private final ObjectTypeMapper objectTypeMapper;

    public InterfaceMapper(InterfaceMappingStrategy interfaceStrategy, ObjectTypeMapper objectTypeMapper) {
        this.interfaceStrategy = interfaceStrategy;
        this.objectTypeMapper = objectTypeMapper;
    }

    @Override
    public GraphQLInterfaceType toGraphQLType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInterfaceType.Builder typeBuilder = newInterface()
                .name(typeName)
                .description(buildContext.typeMetaDataGenerator.generateTypeDescription(javaType));

        buildContext.operationRepository.getChildQueries(javaType)
                .forEach(childQuery -> typeBuilder.field(operationMapper.toGraphQLOperation(childQuery, buildContext)));

        typeBuilder.typeResolver(buildContext.typeResolver);
        return new MappedGraphQLInterfaceType(typeBuilder.build(), javaType);
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return objectTypeMapper.toGraphQLInputType(typeName, javaType, abstractTypes, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }
}
