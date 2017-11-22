package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public interface OperationNameGenerator {

    String generateQueryName(Method queryMethod, AnnotatedType declaringType, Object instance);

    String generateQueryName(Field queryField, AnnotatedType declaringType, Object instance);

    String generateMutationName(Method mutationMethod, AnnotatedType declaringType, Object instance);

    String generateSubscriptionName(Method subscriptionMethod, AnnotatedType declaringType, Object instance);
}