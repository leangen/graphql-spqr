package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonValueMapperFactory implements ValueMapperFactory<JacksonValueMapper> {

    private final String[] basePackages;
    private final ObjectMapper prototype;
    private final Configurer configurer;
    private final TypeInfoGenerator typeInfoGenerator;

    public JacksonValueMapperFactory() {
        this(new String[0]);
    }

    public JacksonValueMapperFactory(String... basePackages) {
        this(basePackages, null, new DefaultTypeInfoGenerator(), new AbstractClassAdapterConfigurer());
    }

    private JacksonValueMapperFactory(String[] basePackages, ObjectMapper prototype, TypeInfoGenerator typeInfoGenerator, Configurer configurer) {
        this.basePackages = basePackages;
        this.prototype = prototype;
        this.configurer = Objects.requireNonNull(configurer);
        this.typeInfoGenerator = Objects.requireNonNull(typeInfoGenerator);
    }

    @Override
    public JacksonValueMapper getValueMapper(Set<Type> abstractTypes, GlobalEnvironment environment) {
        ObjectMapper mapper = prototype != null ? prototype.copy() : new ObjectMapper();
        ObjectMapper objectMapper = this.configurer.configure(mapper, abstractTypes, basePackages, this.typeInfoGenerator, environment);
        return new JacksonValueMapper(objectMapper);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public ObjectMapper configure(ObjectMapper objectMapper, Set<Type> abstractTypes, String[] basePackages, TypeInfoGenerator metaDataGen, GlobalEnvironment environment) {
            ObjectMapper mapper = objectMapper
                    .findAndRegisterModules()
                    .registerModule(getAnnotationIntrospectorModule(collectSubtypes(abstractTypes, basePackages, metaDataGen)));
            if (environment != null && !environment.getInputConverters().isEmpty()) {
                mapper.registerModule(getDeserializersModule(environment));
            }
            return mapper;
        }

        private Map<Type, List<NamedType>> collectSubtypes(Set<Type> abstractTypes, String[] basePackages, TypeInfoGenerator metaDataGen) {
            Map<Type, List<NamedType>> types = new HashMap<>();
            Set<Class<?>> abstractClasses = abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .collect(Collectors.toSet());
            for (Class abstractClass : abstractClasses) {
                List<NamedType> subTypes = ClassUtils.findImplementations(abstractClass, basePackages).stream()
                        .filter(impl -> !ClassUtils.isAbstract(impl))
                        .map(sub -> new NamedType(sub, metaDataGen.generateTypeName(GenericTypeReflector.annotate(sub))))
                        .collect(Collectors.toList());
                types.put(abstractClass, subTypes);
            }
            return types;
        }

        private Module getDeserializersModule(GlobalEnvironment environment) {
            return new Module() {
                @Override
                public String getModuleName() {
                    return "graphql-spqr-deserializers";
                }

                @Override
                public Version version() {
                    return Version.unknownVersion();
                }

                @Override
                public void setupModule(SetupContext setupContext) {
                    setupContext.addDeserializers(new ConvertingDeserializers(environment));
                }
            };
        }

        private Module getAnnotationIntrospectorModule(Map<Type, List<NamedType>> typeMap) {
            return new SimpleModule("graphql-spqr-annotation-introspector") {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.insertAnnotationIntrospector(new AnnotationIntrospector(typeMap));
                }
            };
        }
    }

    @FunctionalInterface
    public interface Configurer {
        ObjectMapper configure(ObjectMapper objectMapper, Set<Type> abstractTypes, String[] basePackages, TypeInfoGenerator metaDataGen, GlobalEnvironment environment);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + typeInfoGenerator.getClass().getSimpleName();
    }

    public static class Builder {

        private String[] basePackages;
        private Configurer configurer = new AbstractClassAdapterConfigurer();
        private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
        private ObjectMapper prototype;

        public Builder withBasePackages(String... basePackages) {
            this.basePackages = basePackages;
            return this;
        }

        public Builder withConfigurer(Configurer configurer) {
            this.configurer = configurer;
            return this;
        }

        public Builder withTypeInfoGenerator(TypeInfoGenerator typeInfoGenerator) {
            this.typeInfoGenerator = typeInfoGenerator;
            return this;
        }

        public Builder withPrototype(ObjectMapper prototype) {
            this.prototype = prototype;
            return this;
        }

        public JacksonValueMapperFactory build() {
            return new JacksonValueMapperFactory(basePackages, prototype, typeInfoGenerator, configurer);
        }
    }
}
