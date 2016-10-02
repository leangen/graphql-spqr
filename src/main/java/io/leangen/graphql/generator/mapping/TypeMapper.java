package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public interface TypeMapper {

    GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator);
    GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator);

    boolean supports(AnnotatedType type);
}
