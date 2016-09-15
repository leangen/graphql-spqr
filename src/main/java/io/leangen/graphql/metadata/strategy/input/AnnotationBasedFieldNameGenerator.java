package io.leangen.graphql.metadata.strategy.input;

import io.leangen.graphql.annotations.GraphQLQuery;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public class AnnotationBasedFieldNameGenerator implements FieldNameGenerator {

    @Override
    public Optional<String> generateFieldName(Field field) {
        if (field.isAnnotationPresent(GraphQLQuery.class) && !field.getAnnotation(GraphQLQuery.class).name().isEmpty()) {
            return Optional.of(field.getAnnotation(GraphQLQuery.class).name());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> generateFieldName(Method fieldGetter) {
        if (fieldGetter.isAnnotationPresent(GraphQLQuery.class) && !fieldGetter.getAnnotation(GraphQLQuery.class).name().isEmpty()) {
            return Optional.of(fieldGetter.getAnnotation(GraphQLQuery.class).name());
        }
        return Optional.empty();
    }
}
