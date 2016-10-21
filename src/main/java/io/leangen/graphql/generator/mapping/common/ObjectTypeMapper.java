package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.Optional;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.strategy.AbstractTypeGenerationStrategy;
import io.leangen.graphql.metadata.DomainType;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public class ObjectTypeMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        DomainType domainType = new DomainType(javaType);
        Optional<Query> relayId = buildContext.queryRepository.getDomainQueries(domainType.getJavaType()).stream()
                .filter(query -> query.getJavaType().isAnnotationPresent(RelayId.class))
                .findFirst();

        if (relayId.isPresent()) {
            buildContext.proxyFactory.registerType(ClassUtils.getRawType(domainType.getJavaType().getType()));
        }

        AbstractTypeGenerationStrategy.Entry typeEntry = buildContext.typeStrategy.get(domainType);
        if (typeEntry.type.isPresent()) {
            return typeEntry.type.get();
        }

        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(typeEntry.name)
                .description(domainType.getDescription());

        buildContext.queryRepository.getChildQueries(domainType.getJavaType())
                .forEach(childQuery -> typeBuilder.field(queryGenerator.toGraphQLQuery(childQuery, typeEntry.name, buildContext)));

        if (relayId.isPresent()) {
            typeBuilder.withInterface(queryGenerator.node);
        }
        ClassUtils.getInterfaces(javaType).forEach(
                inter -> typeBuilder.withInterface((GraphQLInterfaceType) queryGenerator.toGraphQLType(inter, buildContext)));

        GraphQLObjectType type = typeBuilder.build();
        buildContext.typeRepository.registerType(domainType, type);
        return type;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
//        Optional<GraphQLInputType> cached = buildContext.typeRepository.getInputType(javaType.getType());
//        if (cached.isPresent()) {
//            return cached.get();
//        }

        DomainType domainType = new DomainType(javaType);

        if (buildContext.inputsInProgress.contains(domainType.getInputName())) {
            return new GraphQLTypeReference(domainType.getInputName());
        }
        buildContext.inputsInProgress.add(domainType.getInputName());

        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(domainType.getInputName())
                .description(domainType.getDescription());

        buildContext.queryRepository.getInputDomainQueries(domainType.getJavaType()).stream()
                .filter(query -> query.getArguments().size() == 0)
                .forEach(
                        field -> typeBuilder.field(queryGenerator.toGraphQLInputField(field, buildContext))
                );

        GraphQLInputObjectType type = typeBuilder.build();
        buildContext.typeRepository.registerType(domainType.getJavaType().getType(), type);
        return type;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
