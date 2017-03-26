package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.util.ClassUtils;

/**
 * Created by bojan.tomic on 7/3/16.
 */
public class AnnotatedOperationNameGenerator implements OperationNameGenerator {

    public String generateQueryName(Method queryMethod, AnnotatedType declaringType) {
        return queryName(queryMethod, declaringType);
    }

    @Override
    public String generateQueryName(Field queryField, AnnotatedType declaringType) {
        return queryName(queryField, declaringType);
    }

    private String queryName(AnnotatedElement resolver, AnnotatedType declaringType) {
        if (resolver.isAnnotationPresent(GraphQLQuery.class)) {
            return resolver.getAnnotation(GraphQLQuery.class).name();
        }
        Class<?> declaringClass = ClassUtils.getRawType(declaringType.getType());
        if (declaringClass.isAnnotationPresent(GraphQLQuery.class)) {
            return declaringClass.getDeclaringClass().getAnnotation(GraphQLQuery.class).name();
        }
        throw new IllegalArgumentException("Neither the method/field " + resolver.toString() +
                " nor the declaring class are annotated with GraphQLQuery");
    }

    @Override
    public String generateMutationName(Method mutationMethod, AnnotatedType declaringType) {
        if (mutationMethod.isAnnotationPresent(GraphQLMutation.class)) {
            return mutationMethod.getAnnotation(GraphQLMutation.class).name();
        }
        Class<?> declaringClass = ClassUtils.getRawType(declaringType.getType());
        if (declaringClass.isAnnotationPresent(GraphQLMutation.class)) {
            return declaringClass.getAnnotation(GraphQLMutation.class).name();
        }
        throw new IllegalArgumentException("Neither the method " + mutationMethod.toString() +
                " nor the declaring class are annotated with GraphQLQuery");
    }
}
