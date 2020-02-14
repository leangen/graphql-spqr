package io.leangen.graphql.module.common.jackson;

import com.fasterxml.jackson.databind.node.POJONode;
import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class JacksonObjectScalarMapper implements TypeMapper {

    @Override
    public GraphQLScalarType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        return JacksonObjectScalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        if (POJONode.class.equals(javaType.getType())) {
            throw new UnsupportedOperationException(POJONode.class.getSimpleName() + " can not be used as input");
        }
        return toGraphQLType(javaType, mappersToSkip, env);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return JacksonObjectScalars.isScalar(type.getType());
    }
}
