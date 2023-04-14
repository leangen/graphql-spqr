package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.generator.mapping.TypeSubstituter;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
//The substitute type S is reflectively accessed by the default #getSubstituteType impl
@SuppressWarnings("unused")
public abstract class AbstractTypeSubstitutingMapper<S> implements TypeMapper, TypeSubstituter {

    protected final AnnotatedType substituteType;

    public AbstractTypeSubstitutingMapper() {
        substituteType = GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeSubstitutingMapper.class.getTypeParameters()[0]);
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return env.operationMapper.toGraphQLType(getSubstituteType(javaType), mappersToSkip, env);
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return env.operationMapper.toGraphQLInputType(getSubstituteType(javaType), mappersToSkip, env);
    }

    /**
     * Returns the type to map instead of the original. This implementation always returns the type of the
     * generic type parameter {@code S}.
     *
     * @param original The type to be replaced
     * @return The substitute type to use for mapping
     */
    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return substituteType;
    }
}
