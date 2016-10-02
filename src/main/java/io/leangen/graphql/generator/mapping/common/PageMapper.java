package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PageMapper extends ObjectTypeMapper {

    //Pages don't need special treatment here, just extract their real type
    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        return super.toGraphQLType(ClassUtils.getTypeArguments(javaType)[0], buildContext, queryGenerator);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        return super.toGraphQLInputType(ClassUtils.getTypeArguments(javaType)[0], buildContext, queryGenerator);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isSuperType(Page.class, type.getType());
    }
}
