package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
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

public class ObjectTypeMapper implements TypeMapper {

    @Override
    public GraphQLObjectType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType domainType = new DomainType(javaType);
        Optional<Query> relayId = buildContext.queryRepository.getDomainQueries(domainType.getJavaType()).stream()
                .filter(query -> query.getJavaType().isAnnotationPresent(RelayId.class))
                .findFirst();

        AbstractTypeGenerationStrategy.Entry typeEntry = buildContext.typeStrategy.get(domainType);
        if (typeEntry.type.isPresent()) {
            return GraphQLObjectType.reference(typeEntry.type.get().getName());
        }

        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(typeEntry.name)
                .description(domainType.getDescription());

        buildContext.queryRepository.getChildQueries(domainType.getJavaType())
                .forEach(childQuery -> typeBuilder.field(queryGenerator.toGraphQLQuery(childQuery, typeEntry.name, buildContext)));

        Set<String> interfaceNames = new HashSet<>();
        if (relayId.isPresent()) {
            typeBuilder.withInterface(queryGenerator.node);
            interfaceNames.add(queryGenerator.node.getName());
        }
        buildContext.interfaceStrategy.getInterfaces(javaType).forEach(
                inter -> {
                    GraphQLOutputType graphQLInterface = queryGenerator.toGraphQLType(inter, buildContext);
                    typeBuilder.withInterface((GraphQLInterfaceType) graphQLInterface);
                    interfaceNames.add(graphQLInterface.getName());
                });

        GraphQLObjectType type = typeBuilder.build();
        buildContext.typeRepository.registerCovariantTypes(interfaceNames, javaType, type);
        if (!interfaceNames.isEmpty()) {
            buildContext.proxyFactory.registerType(ClassUtils.getRawType(javaType.getType()));
        }
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
//        Optional<GraphQLInputType> cached = buildContext.typeRepository.getInputType(javaType.getType());
//        if (cached.isPresent()) {
//            return cached.get();
//        }

        DomainType domainType = new DomainType(javaType);

        if (buildContext.inputsInProgress.contains(domainType.getInputName())) {
            return GraphQLInputObjectType.reference(domainType.getInputName());
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

        return typeBuilder.build();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
