package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLOutputType;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.generator.union.Union;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class UnionInlineMapper extends UnionMapper {

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip, TypeMappingEnvironment env) {
        GraphQLUnion annotation = javaType.getAnnotation(GraphQLUnion.class);
        List<AnnotatedType> possibleJavaTypes = Arrays.asList(((AnnotatedParameterizedType) javaType).getAnnotatedActualTypeArguments());
        return toGraphQLUnion(env.buildContext.interpolate(annotation.name()), env.buildContext.interpolate(annotation.description()), javaType, possibleJavaTypes, env);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return ClassUtils.isSuperClass(Union.class, type);
    }
}
