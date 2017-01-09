package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.query.relay.Page;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class PageMapper implements TypeMapper {

    //Pages don't need special treatment here, just extract their real type
    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return queryGenerator.toGraphQLType(ClassUtils.getTypeArguments(javaType)[0], abstractTypes, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return queryGenerator.toGraphQLInputType(ClassUtils.getTypeArguments(javaType)[0], abstractTypes, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Page.class, type.getType());
    }
}
