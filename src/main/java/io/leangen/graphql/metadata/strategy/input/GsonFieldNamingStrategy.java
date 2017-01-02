package io.leangen.graphql.metadata.strategy.input;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;
import java.util.Optional;

import io.leangen.graphql.util.ClassUtils;

public class GsonFieldNamingStrategy implements FieldNamingStrategy {

    private FieldNameGenerator fieldNameGenerator;
    private FieldNamingStrategy fallback;

    public GsonFieldNamingStrategy() {
        this(new AnnotationBasedFieldNameGenerator(), FieldNamingPolicy.IDENTITY);
    }

    public GsonFieldNamingStrategy(FieldNameGenerator fieldNameGenerator, FieldNamingStrategy fallback) {
        this.fieldNameGenerator = fieldNameGenerator;
        this.fallback = fallback;
    }

    @Override
    public String translateName(Field field) {
        return fieldNameGenerator.generateFieldName(field)
                .orElse(nameFromGetter(field)
                        .orElse(fallback.translateName(field)));
    }

    private Optional<String> nameFromGetter(Field field) {
        try {
            return fieldNameGenerator.generateFieldName(ClassUtils.findGetter(field.getDeclaringClass(), field.getName()));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}
