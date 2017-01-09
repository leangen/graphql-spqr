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
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.DomainType;
import io.leangen.graphql.metadata.Query;
import io.leangen.graphql.util.ClassUtils;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

public class ObjectTypeMapper implements TypeMapper {

    @Override
    public GraphQLObjectType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType domainType = new DomainType(javaType);
        
        if (buildContext.knownTypes.contains(javaType)) {
            return GraphQLObjectType.reference(domainType.getName());
        }
        buildContext.knownTypes.add(javaType);
        
        //TODO remove the reference to @RelayId here. Instead, check for mapped fields of type GraphQLID
        Optional<Query> relayId = buildContext.queryRepository.getDomainQueries(domainType.getJavaType()).stream()
                .filter(query -> query.getJavaType().isAnnotationPresent(RelayId.class))
                .findFirst();

        GraphQLObjectType.Builder typeBuilder = newObject()
                .name(domainType.getName())
                .description(domainType.getDescription());

        buildContext.queryRepository.getChildQueries(domainType.getJavaType())
                .forEach(childQuery -> typeBuilder.field(queryGenerator.toGraphQLQuery(childQuery, domainType.getName(), buildContext)));

        Set<String> interfaceNames = new HashSet<>();
        if (relayId.isPresent()) {
            typeBuilder.withInterface(queryGenerator.node);
            interfaceNames.add(queryGenerator.node.getName());
        }
        buildContext.interfaceStrategy.getInterfaces(javaType).forEach(
                inter -> {
                    GraphQLOutputType graphQLInterface = queryGenerator.toGraphQLType(inter, abstractTypes, buildContext);
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
    public GraphQLInputObjectType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        DomainType domainType = new DomainType(javaType);

        if (buildContext.knownInputTypes.contains(javaType)) {
            return GraphQLInputObjectType.reference(domainType.getInputName());
        }
        buildContext.knownInputTypes.add(javaType);

        GraphQLInputObjectType.Builder typeBuilder = newInputObject()
                .name(domainType.getInputName())
                .description(domainType.getDescription());

        buildContext.queryRepository.getInputDomainQueries(domainType.getJavaType()).stream()
                .filter(query -> query.getArguments().size() == 0)
                .forEach(
                        field -> typeBuilder.field(queryGenerator.toGraphQLInputField(field, abstractTypes, buildContext))
                );

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
