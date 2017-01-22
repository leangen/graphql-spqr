package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public interface FieldNameGenerator {

    Optional<String> generateFieldName(Field field);

    Optional<String> generateFieldName(Method fieldGetter);
}
