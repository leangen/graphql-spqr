package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.util.Scalars;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ScalarJsonMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return Scalars.graphQLJson(javaType);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return Scalars.graphQLJson(javaType);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class);
    }
}
