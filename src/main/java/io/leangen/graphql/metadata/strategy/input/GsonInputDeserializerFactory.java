package io.leangen.graphql.metadata.strategy.input;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.BiConsumer;

import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonInputDeserializerFactory implements InputDeserializerFactory {

    private final FieldNamingStrategy fieldNamingStrategy;
    private final BiConsumer<GsonBuilder, Set<Type>> configurer;
    private final InputDeserializer defaultInputDeserializer;

    public GsonInputDeserializerFactory() {
        this(new GsonFieldNamingStrategy(), new AbstractAdapterConfigurer());
    }

    public GsonInputDeserializerFactory(FieldNamingStrategy fieldNamingStrategy, BiConsumer<GsonBuilder, Set<Type>> configurer) {
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.configurer = configurer;
        this.defaultInputDeserializer = new GsonInputDeserializer(new GsonBuilder().setFieldNamingStrategy(fieldNamingStrategy).create());
    }

    @Override
    public InputDeserializer getDeserializer(Set<Type> abstractTypes) {
        if (abstractTypes.isEmpty()) {
            return defaultInputDeserializer;
        }

        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingStrategy(fieldNamingStrategy);
        configurer.accept(gsonBuilder, abstractTypes);

        return new GsonInputDeserializer(gsonBuilder.create());
    }

    public static class AbstractAdapterConfigurer implements BiConsumer<GsonBuilder, Set<Type>> {

        @Override
        public void accept(GsonBuilder gsonBuilder, Set<Type> abstractTypes) {
            abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .map(GsonInputDeserializerFactory::adapterFor)
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
