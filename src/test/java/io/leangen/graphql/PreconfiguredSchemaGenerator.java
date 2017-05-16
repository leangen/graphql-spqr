package io.leangen.graphql;

import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;

import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;

/**
 * For testing use only!
 * A schema generator that transparently forces global base package to be "io.leangen".
 */
public class PreconfiguredSchemaGenerator extends GraphQLSchemaGenerator {

    private static final String basePackage = "io.leangen";

    public PreconfiguredSchemaGenerator() {
        withBasePackage(basePackage);
    }

    @Override
    public GraphQLSchemaGenerator withValueMapperFactory(ValueMapperFactory valueMapperFactory) {
        if (valueMapperFactory instanceof JacksonValueMapperFactory) {
            valueMapperFactory = clone((JacksonValueMapperFactory) valueMapperFactory);
        } else if (valueMapperFactory instanceof GsonValueMapperFactory) {
            valueMapperFactory = clone((GsonValueMapperFactory) valueMapperFactory);
        }
        return super.withValueMapperFactory(valueMapperFactory);
    }

    private JacksonValueMapperFactory clone(JacksonValueMapperFactory valueMapperFactory) {
        TypeInfoGenerator infoGen = getFieldValue(valueMapperFactory, "typeInfoGenerator");
        JacksonValueMapperFactory.Configurer configurer = getFieldValue(valueMapperFactory, "configurer");
        return new JacksonValueMapperFactory(basePackage, infoGen, configurer);
    }

    private GsonValueMapperFactory clone(GsonValueMapperFactory valueMapperFactory) {
        TypeInfoGenerator infoGen = getFieldValue(valueMapperFactory, "typeInfoGenerator");
        GsonValueMapperFactory.Configurer configurer = getFieldValue(valueMapperFactory, "configurer");
        FieldNamingStrategy fieldNamingStrategy = getFieldValue(valueMapperFactory, "fieldNamingStrategy");
        return new GsonValueMapperFactory(basePackage, infoGen, fieldNamingStrategy, configurer);
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
