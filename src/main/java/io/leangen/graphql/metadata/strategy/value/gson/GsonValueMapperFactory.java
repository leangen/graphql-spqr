package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonValueMapperFactory implements ValueMapperFactory<GsonValueMapper> {

    private final String[] basePackages;
    private final FieldNamingStrategy fieldNamingStrategy;
    private final TypeInfoGenerator typeInfoGenerator;
    private final Configurer configurer;

    public GsonValueMapperFactory() {
        this(new String[0]);
    }

    @SuppressWarnings("WeakerAccess")
    public GsonValueMapperFactory(String... basePackages) {
        this(basePackages, new DefaultTypeInfoGenerator(), new GsonFieldNamingStrategy(), new AbstractClassAdapterConfigurer());
    }

    private GsonValueMapperFactory(String[] basePackages, TypeInfoGenerator typeInfoGenerator, FieldNamingStrategy fieldNamingStrategy, Configurer configurer) {
        this.basePackages = basePackages;
        this.fieldNamingStrategy = Objects.requireNonNull(fieldNamingStrategy);
        this.typeInfoGenerator = Objects.requireNonNull(typeInfoGenerator);
        this.configurer = Objects.requireNonNull(configurer);
    }

    @Override
    public GsonValueMapper getValueMapper(Set<Type> abstractTypes, GlobalEnvironment environment) {
        return new GsonValueMapper(initBuilder(this.fieldNamingStrategy, abstractTypes, this.configurer, environment).create());
    }

    public static Builder builder() {
        return new Builder();
    }

    private GsonBuilder initBuilder(FieldNamingStrategy fieldNamingStrategy, Set<Type> abstractTypes, Configurer configurer, GlobalEnvironment environment) {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingStrategy(fieldNamingStrategy)
                .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory());
        return configurer.configure(gsonBuilder, abstractTypes, basePackages, this.typeInfoGenerator, environment);
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, String[] basePackages, TypeInfoGenerator infoGenerator, GlobalEnvironment environment) {
            abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .map(abstractType -> adapterFor(abstractType, basePackages, infoGenerator))
                    .filter(Objects::nonNull)
                    .forEach(gsonBuilder::registerTypeAdapterFactory);

            if (environment != null && !environment.getInputConverters().isEmpty()) {
                gsonBuilder.registerTypeAdapterFactory(new ConvertingAdapterFactory(environment));
            }

            return gsonBuilder;
        }

        @SuppressWarnings("unchecked")
        private TypeAdapterFactory adapterFor(Class superClass, String[] basePackages, TypeInfoGenerator infoGen) {
            RuntimeTypeAdapterFactory adapterFactory = RuntimeTypeAdapterFactory.of(superClass, ValueMapper.TYPE_METADATA_FIELD_NAME);
            List<Class> implementations = ClassUtils.findImplementations(superClass, basePackages);
            if (implementations.isEmpty()) {
                return null;
            }
            implementations.stream()
                    .filter(impl -> !ClassUtils.isAbstract(impl))
                    .forEach(impl -> adapterFactory.registerSubtype(impl, infoGen.generateTypeName(GenericTypeReflector.annotate(impl))));

            return adapterFactory;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        GsonBuilder configure(GsonBuilder gsonBuilder, Set<Type> abstractTypes, String[] basePackages, TypeInfoGenerator infoGenerator, GlobalEnvironment environment);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + typeInfoGenerator.getClass().getSimpleName();
    }

    public static class Builder {

        private String[] basePackages;
        private FieldNamingStrategy fieldNamingStrategy = new GsonFieldNamingStrategy();
        private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
        private Configurer configurer = new AbstractClassAdapterConfigurer();

        public Builder withBasePackages(String... basePackages) {
            this.basePackages = basePackages;
            return this;
        }

        public Builder withFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
            this.fieldNamingStrategy = fieldNamingStrategy;
            return this;
        }

        public Builder withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator) {
            this.typeInfoGenerator = typeInfoGenerator;
            return this;
        }

        public Builder withConfigurer(Configurer configurer) {
            this.configurer = configurer;
            return this;
        }

        public GsonValueMapperFactory build() {
            return new GsonValueMapperFactory(basePackages, typeInfoGenerator, fieldNamingStrategy, configurer);
        }
    }
}
