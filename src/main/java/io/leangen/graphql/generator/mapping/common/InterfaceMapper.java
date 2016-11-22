package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.strategy.AbstractTypeGenerationStrategy;
import io.leangen.graphql.generator.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.generator.types.MappedGraphQLInterfaceType;
import io.leangen.graphql.metadata.DomainType;

import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InterfaceMapper extends ObjectTypeMapper {

    private final InterfaceMappingStrategy interfaceStrategy;

    public InterfaceMapper(InterfaceMappingStrategy interfaceStrategy) {
        this.interfaceStrategy = interfaceStrategy;
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType domainType = new DomainType(javaType);

        AbstractTypeGenerationStrategy.Entry typeEntry = buildContext.typeStrategy.get(domainType);
        if (typeEntry.type.isPresent()) {
            return typeEntry.type.get();
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
    public boolean supports(AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }
}
