package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JacksonValueMapper implements ValueMapper, InputFieldDiscoveryStrategy {

    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(JacksonValueMapper.class);

    JacksonValueMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T fromInput(Object graphQlInput, Type sourceType, AnnotatedType outputType) {
        return objectMapper.convertValue(graphQlInput, objectMapper.getTypeFactory().constructType(outputType.getType()));
    }

    @Override
    public <T> T fromString(String json, AnnotatedType type) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type.getType()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString(Object output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<InputField> getInputFields(AnnotatedType type, InclusionStrategy inclusionStrategy) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(type.getType());
        BeanDescription desc = objectMapper.getDeserializationConfig().introspect(javaType);
        return desc.findProperties().stream()
                .filter(BeanPropertyDefinition::couldDeserialize)
                .flatMap(prop -> toInputField(type, prop, inclusionStrategy))
                .collect(Collectors.toSet());
    }

    private Stream<InputField> toInputField(AnnotatedType type, BeanPropertyDefinition prop, InclusionStrategy inclusionStrategy) {
        AnnotatedParameter ctorParam = prop.getConstructorParameter();
        if (ctorParam != null) {
            Constructor<?> constructor = (Constructor<?>) ctorParam.getOwner().getMember();
            AnnotatedType fieldType = ClassUtils.getParameterTypes(constructor, type)[ctorParam.getIndex()];
            return inclusionStrategy.includeInputField(
                    constructor.getDeclaringClass(), constructor.getParameters()[ctorParam.getIndex()], fieldType)
                    ? toInputField(prop, fieldType, ctorParam) : Stream.empty();
        }
        if (prop.getSetter() != null) {
            Method setter = prop.getSetter().getAnnotated();
            AnnotatedType fieldType = ClassUtils.getParameterTypes(setter, type)[0];
            return inclusionStrategy.includeInputField(setter.getDeclaringClass(), setter, fieldType)
                    ? toInputField(prop, fieldType, prop.getSetter()) : Stream.empty();
        }
        if (prop.getGetter() != null) {
            Method getter = prop.getGetter().getAnnotated();
            AnnotatedType fieldType = ClassUtils.getReturnType(getter, type);
            return inclusionStrategy.includeInputField(getter.getDeclaringClass(), getter, fieldType)
                    ? toInputField(prop, fieldType, prop.getGetter()) : Stream.empty();
        }
        if (prop.getField() != null) {
            Field field = prop.getField().getAnnotated();
            AnnotatedType fieldType = ClassUtils.getFieldType(field, type);
            return inclusionStrategy.includeInputField(field.getDeclaringClass(), field, fieldType)
                    ? toInputField(prop, fieldType, prop.getField()) : Stream.empty();
        }
        throw new TypeMappingException("Unknown input field mapping style encountered");
    }

    private Stream<InputField> toInputField(BeanPropertyDefinition prop, AnnotatedType fieldType, Annotated accessor) {
        AnnotatedType deserializableType = resolveDeserializableType(accessor, fieldType, accessor.getType(), objectMapper);
        return Stream.of(new InputField(prop.getName(), prop.getMetadata().getDescription(), fieldType, deserializableType));
    }

    private AnnotatedType resolveDeserializableType(Annotated accessor, AnnotatedType realType, JavaType baseType, ObjectMapper objectMapper) {
        AnnotationIntrospector introspector = objectMapper.getDeserializationConfig().getAnnotationIntrospector();
        try {
            objectMapper.getDeserializationContext().getFactory().mapAbstractType(objectMapper.getDeserializationConfig(), objectMapper.constructType(Map.class));
            JavaType refined = introspector.refineDeserializationType(objectMapper.getDeserializationConfig(), accessor, baseType);
            Class<?> raw = ClassUtils.getRawType(realType.getType());
            if (!refined.getRawClass().equals(raw)) {
                if (GenericTypeReflector.isSuperType(realType.getType(), refined.getRawClass())) {
                    AnnotatedType candidate = GenericTypeReflector.getExactSubType(realType, refined.getRawClass());
                    if (!GenericTypeReflector.isMissingTypeParameters(candidate.getType())) {
                        return candidate;
                    }
                }
                return GenericTypeReflector.updateAnnotations(TypeUtils.toJavaType(refined), realType.getAnnotations());
            }
        } catch (JsonMappingException e) {
            /*no-op*/
        } catch (Exception e) {
            log.warn("Failed to determine the deserializable type for " + GenericTypeReflector.getTypeName(realType.getType())
                    + " due to an exception", e);
        }
        return realType;
    }
}
