package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.annotations.GraphQLScalar;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Scalars;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class ObjectScalarMapper extends CachingMapper<GraphQLScalarType, GraphQLScalarType> implements Comparator<AnnotatedType> {

    @Override
    public GraphQLScalarType toGraphQLType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        BuildContext buildContext = env.buildContext;

        GraphQLAppliedDirective[] directives = buildContext.directiveBuilder.buildScalarTypeDirectives(javaType, buildContext.directiveBuilderParams()).stream()
                .map(directive -> env.operationMapper.toGraphQLAppliedDirective(directive, buildContext))
                .toArray(GraphQLAppliedDirective[]::new);
        return ClassUtils.isSuperClass(Map.class, javaType)
                ? Scalars.graphQLMapScalar(typeName, directives)
                : Scalars.graphQLObjectScalar(typeName, directives);
    }

    @Override
    public GraphQLScalarType toGraphQLInputType(String typeName, AnnotatedType javaType, TypeMappingEnvironment env) {
        return toGraphQLType(typeName, javaType, env);
    }

    @Override
    public boolean supports(AnnotatedElement element, AnnotatedType type) {
        return type.isAnnotationPresent(GraphQLScalar.class)
                || Object.class.equals(type.getType())
                || ClassUtils.isSuperClass(Map.class, type);
    }

    @Override
    public int compare(AnnotatedType o1, AnnotatedType o2) {
        Set<Class<? extends Annotation>> scalarAnnotation = Collections.singleton(GraphQLScalar.class);
        return ClassUtils.removeAnnotations(o1, scalarAnnotation).equals(ClassUtils.removeAnnotations(o2, scalarAnnotation)) ? 0 : -1;
    }
}
