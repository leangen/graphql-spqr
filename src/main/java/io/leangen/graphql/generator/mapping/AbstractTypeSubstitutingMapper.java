package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLOutputType;
import io.leangen.gentyref8.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.QueryGenerator;
import io.leangen.graphql.generator.mapping.common.ObjectTypeMapper;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
//The substitute type S is reflectively accessed by the default #getSubstituteType impl
@SuppressWarnings("unused")
public abstract class AbstractTypeSubstitutingMapper<S> extends ObjectTypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, BuildContext buildContext, QueryGenerator queryGenerator) {
        return super.toGraphQLType(getSubstituteType(javaType), buildContext, queryGenerator);
    }

    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeSubstitutingMapper.class.getTypeParameters()[0]);
    }
}
