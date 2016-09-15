package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 7/3/16.
 */
public class BeanQueryNameGenerator implements QueryNameGenerator {

    @Override
    public String generateQueryName(Method queryMethod, AnnotatedType declaringType) {
        if (ClassUtils.isGetter(queryMethod)) {
            return ClassUtils.getFieldNameFromGetter(queryMethod);
        }
        throw new IllegalArgumentException("Method " + queryMethod.toString() + " does not conform to the Java Bean specification");
    }

    @Override
    public String generateQueryName(Field domainField, AnnotatedType declaringType) {
        return domainField.getName();
    }

    @Override
    public String generateMutationName(Method mutationMethod, AnnotatedType declaringType) {
        if (ClassUtils.isSetter(mutationMethod)) {
            return ClassUtils.getFieldNameFromSetter(mutationMethod);
        }
        throw new IllegalArgumentException("Method " + mutationMethod.toString() + " does not conform to the Java Bean specification");
    }
}
