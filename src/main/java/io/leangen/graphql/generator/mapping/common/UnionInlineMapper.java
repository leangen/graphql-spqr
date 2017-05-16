package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import graphql.schema.GraphQLOutputType;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.union.Union;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class UnionInlineMapper extends UnionMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Type> abstractTypes, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLUnion annotation = javaType.getAnnotation(GraphQLUnion.class);
        List<AnnotatedType> possibleJavaTypes = Arrays.asList(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments());
        return toGraphQLUnion(annotation.name(), annotation.description(), javaType, possibleJavaTypes, abstractTypes, operationMapper, buildContext);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Union.class, type.getType());
    }
}
