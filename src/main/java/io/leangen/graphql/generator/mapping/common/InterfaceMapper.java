package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.strategy.AbstractTypeGenerationStrategy;
import io.leangen.graphql.generator.strategy.InterfaceMappingStrategy;
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
    public GraphQLInterfaceType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType domainType = new DomainType(javaType);

        AbstractTypeGenerationStrategy.Entry typeEntry = buildContext.typeStrategy.get(domainType);
        if (typeEntry.type.isPresent()) {
            return GraphQLInterfaceType.reference(typeEntry.type.get().getName());
        }

        GraphQLInterfaceType.Builder typeBuilder = newInterface()
                .name(typeEntry.name)
                .description(domainType.getDescription());

        buildContext.queryRepository.getChildQueries(domainType.getJavaType())
                .forEach(childQuery -> typeBuilder.field(queryGenerator.toGraphQLQuery(childQuery, typeEntry.name, buildContext)));

        typeBuilder.typeResolver(buildContext.typeResolver);
        return new MappedGraphQLInterfaceType(typeBuilder.build(), javaType);
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return objectTypeMapper.toGraphQLInputType(javaType, queryGenerator, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }
}
