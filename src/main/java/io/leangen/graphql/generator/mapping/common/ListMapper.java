package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ListMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return new GraphQLList(env.operationMapper.toGraphQLType(getElementType(javaType), env));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return new GraphQLList(env.operationMapper.toGraphQLInputType(getElementType(javaType), env));
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.isSuperClass(Collection.class, type);
    }
    
    private AnnotatedType getElementType(AnnotatedType javaType) {
        return GenericTypeReflector.getTypeParameter(javaType, Collection.class.getTypeParameters()[0]);
    }
}
