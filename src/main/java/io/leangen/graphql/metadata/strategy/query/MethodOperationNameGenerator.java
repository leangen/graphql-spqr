package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public class MethodOperationNameGenerator implements OperationNameGenerator {

    @Override
    public String generateQueryName(Method queryMethod, AnnotatedType type, Object instance) {
        return queryMethod.getName();
    }

    @Override
    public String generateQueryName(Field queryField, AnnotatedType declaringType, Object instance) {
        return queryField.getName();
    }

    @Override
    public String generateMutationName(Method mutationMethod, AnnotatedType declaringType, Object instance) {
        return mutationMethod.getName();
    }

    @Override
    public String generateSubscriptionName(Method subscriptionMethod, AnnotatedType declaringType, Object instance) {
        return subscriptionMethod.getName();
    }
}
