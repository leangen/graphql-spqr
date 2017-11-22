package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public class ReturnTypeOperationNameGenerator implements OperationNameGenerator {

    @Override
    public String generateQueryName(Method queryElement, AnnotatedType type, Object instance) {
        String name = queryElement.getReturnType().getSimpleName().toLowerCase();
        if (queryElement.getReturnType().isArray() || Iterable.class.isAssignableFrom(queryElement.getReturnType())) {
            return pluralize(name);
        }
        return name;
    }

    @Override
    public String generateQueryName(Field queryField, AnnotatedType declaringType, Object instance) {
        String name = queryField.getType().getSimpleName().toLowerCase();
        if (queryField.getType().isArray() || Iterable.class.isAssignableFrom(queryField.getType())) {
            return pluralize(name);
        }
        return name;
    }

    @Override
    public String generateMutationName(Method mutationMethod, AnnotatedType declaringType, Object instance) {
        return generateQueryName(mutationMethod, declaringType, instance);
    }

    @Override
    public String generateSubscriptionName(Method subscriptionMethod, AnnotatedType declaringType, Object instance) {
        return generateQueryName(subscriptionMethod, declaringType, instance);
    }

    @SuppressWarnings("WeakerAccess")
    protected String pluralize(String noun) {
        if (noun.endsWith("man")) {
            return noun.substring(noun.length() - 3) + "en";
        }
        if (noun.endsWith("s") || noun.endsWith("x") || noun.endsWith("ch")) {
            return noun + "es";
        }
        if (noun.endsWith("y")) {
            return noun.substring(noun.length() - 2) + "ies";
        }
        return noun + "s";
    }
}
