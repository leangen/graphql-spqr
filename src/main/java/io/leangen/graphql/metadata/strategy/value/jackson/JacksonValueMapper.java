package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

public class JacksonValueMapper implements ValueMapper, InputFieldDiscoveryStrategy {

    private final ObjectMapper objectMapper;

    public JacksonValueMapper(ObjectMapper objectMapper) {
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
    public Set<InputField> getInputFields(AnnotatedType type) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(type.getType());
        BeanDescription desc = objectMapper.getDeserializationConfig().introspect(javaType);
        return desc.findProperties().stream()
                .filter(BeanPropertyDefinition::couldDeserialize)
                .map(def -> new InputField(def.getName(), def.getMetadata().getDescription(), getInputFieldType(type, def)))
                .collect(Collectors.toSet());
    }

    private AnnotatedType getInputFieldType(AnnotatedType type, BeanPropertyDefinition propertyDefinition) {
        AnnotatedParameter ctorParam = propertyDefinition.getConstructorParameter();
        if (ctorParam != null) {
            Constructor<?> constructor = (Constructor<?>) ctorParam.getOwner().getMember();
            return ClassUtils.getParameterTypes(constructor, type)[ctorParam.getIndex()];
        }
        if (propertyDefinition.getSetter() != null) {
            return ClassUtils.getParameterTypes(propertyDefinition.getSetter().getAnnotated(), type)[0];
        }
        if (propertyDefinition.getGetter() != null) {
            return ClassUtils.getReturnType(propertyDefinition.getGetter().getAnnotated(), type);
        }
        if (propertyDefinition.getField() != null) {
            return ClassUtils.getFieldType(propertyDefinition.getField().getAnnotated(), type);
        }
        throw new UnsupportedOperationException("Unknown input field mapping style encountered");
    }
}
