package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.Set;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonValueMapperFactory implements ValueMapperFactory<GsonValueMapper> {

    private final String basePackage;
    private final FieldNamingStrategy fieldNamingStrategy;
    private final TypeInfoGenerator typeInfoGenerator;
    private final Configurer configurer;

    public GsonValueMapperFactory() {
        this(null);
    }

    public GsonValueMapperFactory(String basePackage) {
        this(basePackage, new DefaultTypeInfoGenerator());
    }

    public GsonValueMapperFactory(String basePackage, TypeInfoGenerator typeInfoGenerator) {
        this(basePackage, typeInfoGenerator, new GsonFieldNamingStrategy(), new AbstractClassAdapterConfigurer());
    }

    public GsonValueMapperFactory(String basePackage, TypeInfoGenerator typeInfoGenerator, FieldNamingStrategy fieldNamingStrategy, Configurer configurer) {
        this.basePackage = basePackage;
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.typeInfoGenerator = typeInfoGenerator;
        this.configurer = configurer;
    }

    @Override
    public GsonValueMapper getValueMapper(Set<Type> abstractTypes, GlobalEnvironment environment) {
        return new GsonValueMapper(initBuilder(this.fieldNamingStrategy, abstractTypes, this.configurer, environment).create());
    }

    private GsonBuilder initBuilder(FieldNamingStrategy fieldNamingStrategy, Set<Type> abstractTypes, Configurer configurer, GlobalEnvironment environment) {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingStrategy(fieldNamingStrategy)
                .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory());
        return configurer.configure(gsonBuilder, abstractTypes, basePackage, this.typeInfoGenerator, environment);
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, String basePackage, TypeInfoGenerator infoGenerator, GlobalEnvironment environment) {
            abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .map(abstractType -> adapterFor(abstractType, basePackage, infoGenerator))
                    .forEach(gsonBuilder::registerTypeAdapterFactory);

            if (environment != null && !environment.getInputConverters().isEmpty()) {
                gsonBuilder.registerTypeAdapterFactory(new ConvertingAdapterFactory(environment));
            }

            return gsonBuilder;
        }

        @SuppressWarnings("unchecked")
        private TypeAdapterFactory adapterFor(Class superClass, String basePackage, TypeInfoGenerator infoGen) {
            RuntimeTypeAdapterFactory adapterFactory = RuntimeTypeAdapterFactory.of(superClass, ValueMapper.TYPE_METADATA_FIELD_NAME);

            ClassUtils.findImplementations(superClass, basePackage).stream()
                    .filter(impl -> !ClassUtils.isAbstract(impl))
                    .forEach(impl -> adapterFactory.registerSubtype(impl, infoGen.generateTypeName(GenericTypeReflector.annotate(impl))));

            return adapterFactory;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, String basePackage, TypeInfoGenerator infoGenerator, GlobalEnvironment environment);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + typeInfoGenerator.getClass().getSimpleName();
    }
}
