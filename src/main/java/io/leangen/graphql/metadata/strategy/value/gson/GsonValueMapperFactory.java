package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapterFactory;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ScalarDeserializationStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class GsonValueMapperFactory implements ValueMapperFactory, ScalarDeserializationStrategy {

    private final Gson prototype;
    private final FieldNamingStrategy fieldNamingStrategy;
    private final TypeInfoGenerator typeInfoGenerator;
    private final List<Configurer> configurers;

    public GsonValueMapperFactory() {
        this(null, new DefaultTypeInfoGenerator(), null, Collections.singletonList(new AbstractClassAdapterConfigurer()));
    }

    private GsonValueMapperFactory(Gson prototype, TypeInfoGenerator typeInfoGenerator, FieldNamingStrategy fieldNamingStrategy, List<Configurer> configurers) {
        this.prototype = prototype;
        this.fieldNamingStrategy = fieldNamingStrategy;
        this.typeInfoGenerator = Objects.requireNonNull(typeInfoGenerator);
        this.configurers = Objects.requireNonNull(configurers);
    }

    @Override
    public GsonValueMapper getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
        return new GsonValueMapper(initBuilder(concreteSubTypes, environment).create());
    }

    private GsonBuilder initBuilder(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
        GsonBuilder gsonBuilder = (prototype != null ? prototype.newBuilder() : new GsonBuilder())
                .serializeNulls()
                .setFieldNamingStrategy(fieldNamingStrategy != null ? fieldNamingStrategy : new GsonFieldNamingStrategy(environment.messageBundle))
                .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory());
        return configurers.stream().reduce(gsonBuilder, (builder, config) ->
                        config.configure(new ConfigurerParams(builder, concreteSubTypes, this.typeInfoGenerator, environment)), (b1, b2) -> b2);
    }

    @Override
    public boolean isDirectlyDeserializable(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(JsonElement.class, type.getType());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public GsonBuilder configure(ConfigurerParams params) {
            params.concreteSubTypes.entrySet().stream()
                    .map(entry -> adapterFor(entry.getKey(), entry.getValue(), params.infoGenerator, params.environment.messageBundle))
                    .filter(Objects::nonNull)
                    .forEach(params.gsonBuilder::registerTypeAdapterFactory);

            if (!params.environment.getInputConverters().isEmpty()) {
                params.gsonBuilder.registerTypeAdapterFactory(new ConvertingAdapterFactory(params.environment));
            }

            return params.gsonBuilder;
        }

        @SuppressWarnings("unchecked")
        private TypeAdapterFactory adapterFor(Class superClass, List<Class<?>> implementations, TypeInfoGenerator infoGen, MessageBundle messageBundle) {
            RuntimeTypeAdapterFactory adapterFactory = RuntimeTypeAdapterFactory.of(superClass, ValueMapper.TYPE_METADATA_FIELD_NAME);
            if (implementations.isEmpty()) {
                return null;
            }
            implementations.stream()
                    .filter(impl -> !ClassUtils.isAbstract(impl))
                    .forEach(impl -> adapterFactory.registerSubtype(impl, infoGen.generateTypeName(GenericTypeReflector.annotate(impl), messageBundle)));

            return adapterFactory;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        GsonBuilder configure(ConfigurerParams params);
    }

    public static class ConfigurerParams {

        final GsonBuilder gsonBuilder;
        final Map<Class, List<Class<?>>> concreteSubTypes;
        final TypeInfoGenerator infoGenerator;
        final GlobalEnvironment environment;

        ConfigurerParams(GsonBuilder gsonBuilder, Map<Class, List<Class<?>>> concreteSubTypes, TypeInfoGenerator infoGenerator, GlobalEnvironment environment) {
            this.gsonBuilder = gsonBuilder;
            this.concreteSubTypes = concreteSubTypes;
            this.infoGenerator = infoGenerator;
            this.environment = environment;
        }

        public GsonBuilder getGsonBuilder() {
            return gsonBuilder;
        }

        public Map<Class, List<Class<?>>> getConcreteSubTypes() {
            return concreteSubTypes;
        }

        public TypeInfoGenerator getInfoGenerator() {
            return infoGenerator;
        }

        public GlobalEnvironment getEnvironment() {
            return environment;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public static class Builder {

        private Gson prototype;
        private FieldNamingStrategy fieldNamingStrategy;
        private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
        private List<Configurer> configurers = new ArrayList<>(Collections.singleton(new AbstractClassAdapterConfigurer()));

        public Builder withFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
            this.fieldNamingStrategy = fieldNamingStrategy;
            return this;
        }

        public Builder withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator) {
            this.typeInfoGenerator = typeInfoGenerator;
            return this;
        }

        public Builder withPrototype(Gson prototype) {
            this.prototype = prototype;
            return this;
        }

        public Builder withConfigurer(Configurer configurer) {
            Collections.addAll(this.configurers, configurer);
            return this;
        }

        public GsonValueMapperFactory build() {
            return new GsonValueMapperFactory(prototype, typeInfoGenerator, fieldNamingStrategy, configurers);
        }
    }
}
