package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonValueMapperFactory implements ValueMapperFactory<GsonValueMapper> {

    private final FieldNamingStrategy fieldNamingStrategy;
    private final TypeInfoGenerator typeInfoGenerator;
    private final Configurer configurer;
    private final GsonValueMapper defaultValueMapper;

    public GsonValueMapperFactory() {
        this(new DefaultTypeInfoGenerator());
    }

    public GsonValueMapperFactory(TypeInfoGenerator typeInfoGenerator) {
        this(typeInfoGenerator, new GsonFieldNamingStrategy(), new AbstractClassAdapterConfigurer());
    }

    public GsonValueMapperFactory(TypeInfoGenerator typeInfoGenerator, FieldNamingStrategy fieldNamingStrategy, Configurer configurer) {
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.typeInfoGenerator = typeInfoGenerator;
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
        return configurer.configure(gsonBuilder, abstractTypes, this.typeInfoGenerator);
    }
    
    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, TypeInfoGenerator metaDataGen) {
            abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .map(abstractType -> adapterFor(abstractType, metaDataGen))
                    .forEach(gsonBuilder::registerTypeAdapterFactory);
            
            return gsonBuilder;
        }

        @SuppressWarnings("unchecked")
        private TypeAdapterFactory adapterFor(Class superClass, TypeInfoGenerator metaDataGen) {
            RuntimeTypeAdapterFactory adapterFactory = RuntimeTypeAdapterFactory.of(superClass, ValueMapper.TYPE_METADATA_FIELD_NAME);

            ClassUtils.findImplementations(superClass).stream()
                    .filter(impl -> !ClassUtils.isAbstract(impl))
                    .forEach(impl -> adapterFactory.registerSubtype(impl, metaDataGen.generateTypeName(GenericTypeReflector.annotate(impl))));

            return adapterFactory;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, TypeInfoGenerator metaDataGen);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + typeInfoGenerator.getClass().getSimpleName();
    }
}
