package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
//The substitute type S is reflectively accessed by the default #getSubstituteType impl
@SuppressWarnings("unused")
public abstract class AbstractTypeSubstitutingMapper<S> implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return operationMapper.toGraphQLType(getSubstituteType(javaType), abstractTypes, buildContext);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return operationMapper.toGraphQLInputType(getSubstituteType(javaType), abstractTypes, buildContext);
    }

    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeSubstitutingMapper.class.getTypeParameters()[0]);
    }
}
