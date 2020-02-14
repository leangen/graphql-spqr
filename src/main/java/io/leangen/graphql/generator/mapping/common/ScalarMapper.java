package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.util.Scalars;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ScalarMapper extends CachingMapper<GraphQLScalarType, GraphQLScalarType> {

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        return Scalars.toGraphQLScalarType(javaType.getType());
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        return toGraphQLType(typeName, javaType, env);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return Scalars.isScalar(type.getType());
    }

    @Override
    protected String getTypeName(AnnotatedType type, BuildContext buildContext) {
        return Scalars.toGraphQLScalarType(type.getType()).getName();
    }

    @Override
    protected String getInputTypeName(AnnotatedType type, BuildContext buildContext) {
        return getTypeName(type, buildContext);
    }
}
