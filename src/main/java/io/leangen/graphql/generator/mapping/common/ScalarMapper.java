package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.util.GraphQLUtils;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ScalarMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        return GraphQLUtils.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        return GraphQLUtils.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GraphQLUtils.isScalar(type.getType());
    }
}
