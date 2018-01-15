package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonValueMapperFactory implements ValueMapperFactory<JacksonValueMapper> {

    private final String basePackage;
    private final Configurer configurer;
    private final TypeInfoGenerator typeInfoGenerator;

    public JacksonValueMapperFactory() {
        this(null);
    }

    public JacksonValueMapperFactory(String basePackage) {
        this(basePackage, new DefaultTypeInfoGenerator());
    }

    public JacksonValueMapperFactory(String basePackage, TypeInfoGenerator typeInfoGenerator) {
        this(basePackage, typeInfoGenerator, new AbstractClassAdapterConfigurer());
    }

    public JacksonValueMapperFactory(String basePackage, TypeInfoGenerator typeInfoGenerator, Configurer configurer) {
        this.basePackage = basePackage;
        this.configurer = configurer;
        this.typeInfoGenerator = typeInfoGenerator;
    }

    @Override
    public JacksonValueMapper getValueMapper(Set<Type> abstractTypes, GlobalEnvironment environment) {
        ObjectMapper objectMapper = this.configurer.configure(new ObjectMapper(), abstractTypes, basePackage, this.typeInfoGenerator, environment);
        return new JacksonValueMapper(objectMapper);
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public ObjectMapper configure(ObjectMapper objectMapper, Set<Type> abstractTypes, String basePackage, TypeInfoGenerator metaDataGen, GlobalEnvironment environment) {
            ObjectMapper mapper = objectMapper
                    .findAndRegisterModules()
                    .setAnnotationIntrospector(new AnnotationIntrospector(collectSubtypes(abstractTypes, basePackage, metaDataGen)));
            if (environment != null && !environment.getInputConverters().isEmpty()) {
                mapper.registerModule(getDeserializersModule(environment));
            }
            return mapper;
        }

        private Map<Type, List<NamedType>> collectSubtypes(Set<Type> abstractTypes, String basePackage, TypeInfoGenerator metaDataGen) {
            Map<Type, List<NamedType>> types = new HashMap<>();
            Set<Class<?>> abstractClasses = abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .collect(Collectors.toSet());
            for (Class abstractClass : abstractClasses) {
                List<NamedType> subTypes = ClassUtils.findImplementations(abstractClass, basePackage).stream()
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
    }

    @FunctionalInterface
    public interface Configurer {
        ObjectMapper configure(ObjectMapper objectMapper, Set<Type> abstractTypes, String basePackage, TypeInfoGenerator metaDataGen, GlobalEnvironment environment);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + typeInfoGenerator.getClass().getSimpleName();
    }
}
