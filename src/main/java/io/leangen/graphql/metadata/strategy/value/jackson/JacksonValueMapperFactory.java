package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ScalarDeserializationStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonValueMapperFactory implements ValueMapperFactory<JacksonValueMapper>, ScalarDeserializationStrategy {

    private final ObjectMapper prototype;
    private final Configurer configurer;
    private final TypeInfoGenerator typeInfoGenerator;

    @SuppressWarnings("WeakerAccess")
    public JacksonValueMapperFactory() {
        this(null, new DefaultTypeInfoGenerator(), new AbstractClassAdapterConfigurer());
    }

    private JacksonValueMapperFactory(ObjectMapper prototype, TypeInfoGenerator typeInfoGenerator, Configurer configurer) {
        this.prototype = prototype;
        this.configurer = Objects.requireNonNull(configurer);
        this.typeInfoGenerator = Objects.requireNonNull(typeInfoGenerator);
    }

    @Override
    public JacksonValueMapper getValueMapper(Map<Class, List<Class>> concreteSubTypes, GlobalEnvironment environment) {
        ObjectMapper mapper = prototype != null ? prototype.copy() : new ObjectMapper();
        ObjectMapper objectMapper = this.configurer.configure(mapper, concreteSubTypes, this.typeInfoGenerator, environment);
        return new JacksonValueMapper(objectMapper);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isDirectlyDeserializable(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(TreeNode.class, type.getType());
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public ObjectMapper configure(ObjectMapper objectMapper, Map<Class, List<Class>> concreteSubTypes, TypeInfoGenerator metaDataGen, GlobalEnvironment environment) {
            ObjectMapper mapper = objectMapper
                    .findAndRegisterModules()
                    .registerModule(getAnnotationIntrospectorModule(unambiguousSubtypes(concreteSubTypes), ambiguousSubtypes(concreteSubTypes, metaDataGen)));
            if (environment != null && !environment.getInputConverters().isEmpty()) {
                mapper.registerModule(getDeserializersModule(environment));
            }
            return mapper;
        }

        private Map<Class, Class> unambiguousSubtypes(Map<Class, List<Class>> concreteSubTypes) {
            return concreteSubTypes.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        }

        private Map<Type, List<NamedType>> ambiguousSubtypes(Map<Class, List<Class>> concreteSubTypes, TypeInfoGenerator metaDataGen) {
            Map<Type, List<NamedType>> types = new HashMap<>();
            concreteSubTypes.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .forEach(entry -> {
                        List<NamedType> subTypes = entry.getValue().stream()
                                .map(sub -> new NamedType(sub, metaDataGen.generateTypeName(GenericTypeReflector.annotate(sub))))
                                .collect(Collectors.toList());
                        if (!subTypes.isEmpty()) {
                            types.put(entry.getKey(), subTypes);
                        }
                    });
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

        private Module getAnnotationIntrospectorModule(Map<Class, Class> unambiguousTypes, Map<Type, List<NamedType>> ambiguousTypes) {
            SimpleModule module = new SimpleModule("graphql-spqr-annotation-introspector") {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.insertAnnotationIntrospector(new AnnotationIntrospector(ambiguousTypes));
                }
            };
            unambiguousTypes.forEach(module::addAbstractTypeMapping);
            return module;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        ObjectMapper configure(ObjectMapper objectMapper, Map<Class, List<Class>> concreteSubTypes, TypeInfoGenerator metaDataGen, GlobalEnvironment environment);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + typeInfoGenerator.getClass().getSimpleName();
    }

    public static class Builder {

        private Configurer configurer = new AbstractClassAdapterConfigurer();
        private TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator();
        private ObjectMapper prototype;

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
            return new JacksonValueMapperFactory(prototype, typeInfoGenerator, configurer);
        }
    }
}
