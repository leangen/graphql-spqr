package io.leangen.graphql;

import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;

import io.leangen.graphql.metadata.strategy.query.DefaultOperationBuilder;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;

/**
 * <b>For testing use only!</b>
 * A schema generator with default configuration useful for testing.
 */
public class TestSchemaGenerator extends GraphQLSchemaGenerator {

    private static final String[] basePackages = new String[] {"io.leangen"};

    public TestSchemaGenerator() {
        withBasePackages(basePackages);
        withOperationBuilder(new DefaultOperationBuilder(DefaultOperationBuilder.TypeInference.LIMITED));
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
        return JacksonValueMapperFactory.builder()
                .withBasePackages(basePackages)
                .withTypeInfoGenerator(infoGen)
                .withConfigurer(configurer)
                .build();
    }

    private GsonValueMapperFactory clone(GsonValueMapperFactory valueMapperFactory) {
        TypeInfoGenerator infoGen = getFieldValue(valueMapperFactory, "typeInfoGenerator");
        GsonValueMapperFactory.Configurer configurer = getFieldValue(valueMapperFactory, "configurer");
        FieldNamingStrategy fieldNamingStrategy = getFieldValue(valueMapperFactory, "fieldNamingStrategy");
        return GsonValueMapperFactory.builder()
                .withBasePackages(basePackages)
                .withTypeInfoGenerator(infoGen)
                .withFieldNamingStrategy(fieldNamingStrategy)
                .withConfigurer(configurer)
                .build();
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
