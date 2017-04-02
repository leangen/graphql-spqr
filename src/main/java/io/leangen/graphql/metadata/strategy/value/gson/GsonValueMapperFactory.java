package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonValueMapperFactory implements ValueMapperFactory<GsonValueMapper> {

    private final FieldNamingStrategy fieldNamingStrategy;
    private final TypeMetaDataGenerator metaDataGenerator;
    private final Configurer configurer;
    private final GsonValueMapper defaultValueMapper;

    public GsonValueMapperFactory(TypeMetaDataGenerator metaDataGenerator) {
        this(metaDataGenerator, new GsonFieldNamingStrategy(), new AbstractClassAdapterConfigurer());
    }

    public GsonValueMapperFactory(TypeMetaDataGenerator metaDataGenerator, FieldNamingStrategy fieldNamingStrategy, Configurer configurer) {
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.metaDataGenerator = metaDataGenerator;
        this.configurer = configurer;
        this.defaultValueMapper = new GsonValueMapper(initBuilder(fieldNamingStrategy, Collections.emptySet(), configurer).create());
    }

    @Override
    public GsonValueMapper getValueMapper(Set<Type> abstractTypes) {
        if (abstractTypes.isEmpty()) {
            return defaultValueMapper;
        }

        return new GsonValueMapper(initBuilder(this.fieldNamingStrategy, abstractTypes, this.configurer).create());
    }

    private GsonBuilder initBuilder(FieldNamingStrategy fieldNamingStrategy, Set<Type> abstractTypes, Configurer configurer) {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingStrategy(fieldNamingStrategy);
        return configurer.configure(gsonBuilder, abstractTypes, this.metaDataGenerator);
    }
    
    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, TypeMetaDataGenerator metaDataGen) {
            abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .map(abstractType -> adapterFor(abstractType, metaDataGen))
                    .forEach(gsonBuilder::registerTypeAdapterFactory);
            
            return gsonBuilder;
        }

        @SuppressWarnings("unchecked")
        private TypeAdapterFactory adapterFor(Class superClass, TypeMetaDataGenerator metaDataGen) {
            RuntimeTypeAdapterFactory adapterFactory = RuntimeTypeAdapterFactory.of(superClass, "_type_");

            ClassUtils.findImplementations(superClass).stream()
                    .filter(impl -> !ClassUtils.isAbstract(impl))
                    .forEach(impl -> adapterFactory.registerSubtype(impl, metaDataGen.generateTypeName(GenericTypeReflector.annotate(impl))));

            return adapterFactory;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, TypeMetaDataGenerator metaDataGen);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + metaDataGenerator.getClass().getSimpleName();
    }
}
