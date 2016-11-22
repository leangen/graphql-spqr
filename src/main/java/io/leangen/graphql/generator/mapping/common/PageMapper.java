package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PageMapper extends ObjectTypeMapper {

    //Pages don't need special treatment here, just extract their real type
    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return super.toGraphQLType(ClassUtils.getTypeArguments(javaType)[0], queryGenerator, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, QueryGenerator queryGenerator, BuildContext buildContext) {
        return super.toGraphQLInputType(ClassUtils.getTypeArguments(javaType)[0], queryGenerator, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Page.class, type.getType());
    }
}
