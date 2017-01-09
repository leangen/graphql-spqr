package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.generator.types.MappedGraphQLInterfaceType;
import io.leangen.graphql.metadata.DomainType;

import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InterfaceMapper implements TypeMapper {

    private final InterfaceMappingStrategy interfaceStrategy;
    private final ObjectTypeMapper objectTypeMapper;

    public InterfaceMapper(InterfaceMappingStrategy interfaceStrategy, ObjectTypeMapper objectTypeMapper) {
        this.interfaceStrategy = interfaceStrategy;
        this.objectTypeMapper = objectTypeMapper;
    }

    @Override
    public GraphQLInterfaceType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType domainType = new DomainType(javaType);

        if (buildContext.knownTypes.contains(javaType)) {
            return GraphQLInterfaceType.reference(domainType.getName());
        }
        buildContext.knownTypes.add(javaType);

        GraphQLInterfaceType.Builder typeBuilder = newInterface()
                .name(domainType.getName())
                .description(domainType.getDescription());

        buildContext.queryRepository.getChildQueries(domainType.getJavaType())
                .forEach(childQuery -> typeBuilder.field(queryGenerator.toGraphQLQuery(childQuery, domainType.getName(), buildContext)));

        typeBuilder.typeResolver(buildContext.typeResolver);
        return new MappedGraphQLInterfaceType(typeBuilder.build(), javaType);
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return objectTypeMapper.toGraphQLInputType(javaType, abstractTypes, queryGenerator, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }
}
