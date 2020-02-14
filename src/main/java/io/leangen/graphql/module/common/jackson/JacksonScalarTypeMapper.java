package io.leangen.graphql.module.common.jackson;

import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class JacksonScalarTypeMapper implements TypeMapper {

    @Override
    public GraphQLScalarType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return JacksonScalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return toGraphQLType(javaType, mappersToSkip, env);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return JacksonScalars.isScalar(type.getType());
    }
}
