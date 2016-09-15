package io.leangen.graphql.metadata.strategy.input;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;

/**
 * Created by bojan.tomic on 5/25/16.
 */
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
        return fieldNameGenerator.generateFieldName(field).orElse(fallback.translateName(field));
    }
}
