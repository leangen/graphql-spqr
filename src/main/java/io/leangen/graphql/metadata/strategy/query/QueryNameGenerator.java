package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public interface QueryNameGenerator {

    String generateQueryName(Method queryMethod, AnnotatedType declaringType);

    String generateQueryName(Field domainField, AnnotatedType declaringType);

    String generateMutationName(Method mutationMethod, AnnotatedType declaringType);
}