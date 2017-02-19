package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.BiConsumer;

import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonValueMapperFactory implements ValueMapperFactory {

    private final FieldNamingStrategy fieldNamingStrategy;
    private final BiConsumer<GsonBuilder, Set<Type>> configurer;
    private final ValueMapper defaultValueMapper;

    public GsonValueMapperFactory() {
        this(new GsonFieldNamingStrategy(), new AbstractClassAdapterConfigurer());
    }

    public GsonValueMapperFactory(FieldNamingStrategy fieldNamingStrategy, BiConsumer<GsonBuilder, Set<Type>> configurer) {
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.configurer = configurer;
        this.defaultValueMapper = new GsonValueMapper(new GsonBuilder().setFieldNamingStrategy(fieldNamingStrategy).create());
    }

    @Override
    public ValueMapper getValueMapper(Set<Type> abstractTypes) {
        if (abstractTypes.isEmpty()) {
            return defaultValueMapper;
        }

        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingStrategy(fieldNamingStrategy);
        configurer.accept(gsonBuilder, abstractTypes);

        return new GsonValueMapper(gsonBuilder.create());
    }

    public static class AbstractClassAdapterConfigurer implements BiConsumer<GsonBuilder, Set<Type>> {

        @Override
        public void accept(GsonBuilder gsonBuilder, Set<Type> abstractTypes) {
            abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .map(GsonValueMapperFactory::adapterFor)
                    .forEach(gsonBuilder::registerTypeAdapterFactory);
        }
    }

    @SuppressWarnings("unchecked")
    private static TypeAdapterFactory adapterFor(Class superClass) {
        RuntimeTypeAdapterFactory adapterFactory = RuntimeTypeAdapterFactory.of(superClass, "_type_");

        ClassUtils.findImplementations(superClass).stream()
                .filter(impl -> !ClassUtils.isAbstract(impl))
                .forEach(adapterFactory::registerSubtype);

        return adapterFactory;
    }
}
