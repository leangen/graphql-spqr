package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;

/**
 * @author Bojan Tomic (kaqqao)
 */
//The substitute type S is reflectively accessed by the default #getSubstituteType impl
@SuppressWarnings("unused")
public abstract class AbstractTypeSubstitutingMapper<S> implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return queryGenerator.toGraphQLType(getSubstituteType(javaType), abstractTypes, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, QueryGenerator queryGenerator, BuildContext buildContext) {
        return queryGenerator.toGraphQLInputType(getSubstituteType(javaType), abstractTypes, buildContext);
    }

    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeSubstitutingMapper.class.getTypeParameters()[0]);
    }
}
