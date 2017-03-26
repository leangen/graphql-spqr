package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.TypeMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ListMapper implements TypeMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return new GraphQLList(operationMapper.toGraphQLType(getElementType(javaType), abstractTypes, buildContext));
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        return new GraphQLList(operationMapper.toGraphQLInputType(getElementType(javaType), abstractTypes, buildContext));
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Collection.class, type.getType());
    }
    
    private AnnotatedType getElementType(AnnotatedType javaType) {
        return GenericTypeReflector.getTypeParameter(javaType, Collection.class.getTypeParameters()[0]);
    }
}
