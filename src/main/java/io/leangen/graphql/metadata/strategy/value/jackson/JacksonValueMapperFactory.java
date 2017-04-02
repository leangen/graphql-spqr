package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class JacksonValueMapperFactory implements ValueMapperFactory<JacksonValueMapper> {

    private final Configurer configurer;
    private final TypeMetaDataGenerator metaDataGenerator;
    private final JacksonValueMapper defaultValueMapper;

    public JacksonValueMapperFactory(TypeMetaDataGenerator metaDataGenerator) {
        this(metaDataGenerator, new AbstractClassAdapterConfigurer());
    }

    public JacksonValueMapperFactory(TypeMetaDataGenerator metaDataGenerator, Configurer configurer) {
        this.configurer = configurer;
        this.metaDataGenerator = metaDataGenerator;
        this.defaultValueMapper = new JacksonValueMapper(
                this.configurer.configure(new ObjectMapper(), Collections.emptySet(), metaDataGenerator));
    }

    @Override
    public JacksonValueMapper getValueMapper(Set<Type> abstractTypes) {
        if (abstractTypes.isEmpty()) {
            return this.defaultValueMapper;
        }
        ObjectMapper objectMapper = this.configurer.configure(new ObjectMapper(), abstractTypes, this.metaDataGenerator);
        return new JacksonValueMapper(objectMapper);
    }

    public static class AbstractClassAdapterConfigurer implements Configurer {

        @Override
        public ObjectMapper configure(ObjectMapper objectMapper, Set<Type> abstractTypes, TypeMetaDataGenerator metaDataGen) {
            return objectMapper.setAnnotationIntrospector(new AnnotationIntrospector(collectSubtypes(abstractTypes, metaDataGen)));
        }

        private Map<Type, List<NamedType>> collectSubtypes(Set<Type> abstractTypes, TypeMetaDataGenerator metaDataGen) {
            Map<Type, List<NamedType>> types = new HashMap<>();
            Set<Class<?>> abstractClasses = abstractTypes.stream()
                    .map(ClassUtils::getRawType)
                    .distinct()
                    .collect(Collectors.toSet());
            for (Class abstractClass : abstractClasses) {
                List<NamedType> subTypes = ClassUtils.findImplementations(abstractClass).stream()
                        .filter(impl -> !ClassUtils.isAbstract(impl))
                        .map(sub -> new NamedType(sub, metaDataGen.generateTypeName(GenericTypeReflector.annotate(sub))))
                        .collect(Collectors.toList());
                types.put(abstractClass, subTypes);
            }
            return types;
        }
    }

    @FunctionalInterface
    public interface Configurer {
        ObjectMapper configure(ObjectMapper objectMapper, Set<Type> abstractTypes, TypeMetaDataGenerator metaDataGen);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " with " + metaDataGenerator.getClass().getSimpleName();
    }
}
