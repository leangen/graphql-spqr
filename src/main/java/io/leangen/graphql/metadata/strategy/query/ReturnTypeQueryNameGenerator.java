package io.leangen.graphql.metadata.strategy.query;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by bojan.tomic on 3/11/16.
 */
public class ReturnTypeQueryNameGenerator implements QueryNameGenerator {

    @Override
    public String generateQueryName(Method queryElement, AnnotatedType type) {
        String name = queryElement.getReturnType().getSimpleName().toLowerCase();
        if (Iterable.class.isAssignableFrom(queryElement.getReturnType())) {
            return pluralize(name);
        }
        return name;
    }

    @Override
    public String generateQueryName(Field domainField, AnnotatedType declaringType) {
        String name = domainField.getType().getSimpleName().toLowerCase();
        if (Iterable.class.isAssignableFrom(domainField.getType())) {
            return pluralize(name);
        }
        return name;
    }

    @Override
    public String generateMutationName(Method mutationMethod, AnnotatedType declaringType) {
        return generateQueryName(mutationMethod, declaringType);
    }

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
