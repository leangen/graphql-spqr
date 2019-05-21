package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.Directives;

import java.lang.reflect.AnnotatedType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static graphql.schema.GraphQLUnionType.newUnionType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class UnionMapper implements TypeMapper {

    @SuppressWarnings("WeakerAccess")
    protected GraphQLOutputType toGraphQLUnion(String name, String description, AnnotatedType javaType, List<AnnotatedType> possibleJavaTypes,
                                               OperationMapper operationMapper, BuildContext buildContext) {

        if (buildContext.typeCache.contains(name)) {
            return new GraphQLTypeReference(name);
        }
        buildContext.typeCache.register(name);
        GraphQLUnionType.Builder builder = newUnionType()
                .name(name)
                .description(description);

        Set<String> seen = new HashSet<>(possibleJavaTypes.size());

        possibleJavaTypes.forEach(possibleJavaType -> {
            GraphQLOutputType possibleType = operationMapper.toGraphQLType(possibleJavaType, buildContext);
            if (!seen.add(possibleType.getName())) {
                throw new TypeMappingException("Duplicate possible type " + possibleType.getName() + " for union " + name);
            }

            if (possibleType instanceof GraphQLObjectType) {
                builder.possibleType((GraphQLObjectType) possibleType);
            } else if (possibleType instanceof GraphQLTypeReference) {
                builder.possibleType((GraphQLTypeReference) possibleType);
            } else {
                throw new TypeMappingException(possibleType.getClass().getSimpleName() +
                        " is not a valid GraphQL union member. Only object types can be unionized.");
            }

            buildContext.typeRegistry.registerCovariantType(name, possibleJavaType, possibleType);
        });

        builder.withDirective(Directives.mappedType(javaType));
        buildContext.directiveBuilder.buildUnionTypeDirectives(javaType, buildContext.directiveBuilderParams()).forEach(directive ->
                builder.withDirective(operationMapper.toGraphQLDirective(directive, buildContext)));

        GraphQLUnionType union = builder.build();
        buildContext.codeRegistry.typeResolver(union, buildContext.typeResolver);
        return union;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, OperationMapper operationMapper, Set<Class<? extends TypeMapper>> mappersToSkip, BuildContext buildContext) {
        throw new UnsupportedOperationException("GraphQL union type can not be used as an input type");
    }
}
