package io.leangen.graphql.generator.mapping.common;

import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.annotations.RelayId;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class RelayIdMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        return Scalars.GraphQLID;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        return Scalars.GraphQLID;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return type.isAnnotationPresent(RelayId.class);
    }
}
