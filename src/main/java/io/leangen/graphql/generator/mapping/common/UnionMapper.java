package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.List;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLUnionType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.exceptions.TypeMappingException;
import io.leangen.graphql.generator.mapping.TypeMapper;

import static graphql.schema.GraphQLUnionType.newUnionType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class UnionMapper implements TypeMapper {

    protected GraphQLUnionType toGraphQLUnion(String name, String description, List<AnnotatedType> possibleJavaTypes,
                                            QueryGenerator queryGenerator, BuildContext buildContext) {

        GraphQLUnionType.Builder builder = newUnionType()
                .name(name)
                .description(description)
                .typeResolver(buildContext.typeResolver);

        possibleJavaTypes.stream()
                .map(pos -> queryGenerator.toGraphQLType(pos, buildContext))
                .forEach(type -> {
                    if (type instanceof GraphQLObjectType) {
                        builder.possibleType((GraphQLObjectType) type);
                    } else {
                        throw new TypeMappingException(type.getClass().getSimpleName() +
                                " is not a valid GraphQL union member. Only object types can be unionized.");
                    }
                });

        GraphQLUnionType union = builder.build();
        for (int i = 0; i < possibleJavaTypes.size(); i++) {
            buildContext.typeRepository.registerCovariantTypes(union.getName(), possibleJavaTypes.get(i), union.getTypes().get(i));
        }
        return union;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        throw new UnsupportedOperationException("GraphQL union type can not be used as input type");
    }
}
