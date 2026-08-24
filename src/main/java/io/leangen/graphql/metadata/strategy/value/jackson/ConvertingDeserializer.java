package io.leangen.graphql.metadata.strategy.value.jackson;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;

public class ConvertingDeserializer extends ValueDeserializer {

    private final AnnotatedType detectedType;
    private final JavaType substituteType;
    private final InputConverter inputConverter;
    private final GlobalEnvironment environment;
    private final ValueMapper valueMapper;
    private final ObjectMapper objectMapper;

    public ConvertingDeserializer(InputConverter inputConverter, GlobalEnvironment environment, ObjectMapper objectMapper) {
        this.detectedType = null;
        this.substituteType = null;
        this.inputConverter = inputConverter;
        this.environment = environment;
        this.valueMapper = null;
        this.objectMapper = objectMapper;
    }

    private ConvertingDeserializer(AnnotatedType detectedType, JavaType substituteType, InputConverter inputConverter, GlobalEnvironment environment, ObjectMapper objectMapper) {
        this.detectedType = detectedType;
        this.substituteType = substituteType;
        this.inputConverter = inputConverter;
        this.environment = environment;
        this.valueMapper = new JacksonValueMapper(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) {
        JavaType javaType = deserializationContext.getContextualType() != null ? deserializationContext.getContextualType() : extractType(beanProperty.getMember());
        Annotation[] annotations = annotations(beanProperty);
        AnnotatedType detectedType = environment.typeTransformer.transform(ClassUtils.addAnnotations(TypeUtils.toJavaType(javaType), annotations));
        JavaType substituteType = deserializationContext.getTypeFactory().constructType(environment.getMappableInputType(detectedType).getType());
        if (inputConverter.supports(detectedType)) {
            return new ConvertingDeserializer(detectedType, substituteType, inputConverter, environment, objectMapper);
        } else {
            return new DefaultDeserializer(javaType);
        }
    }

    private Annotation[] annotations(BeanProperty beanProperty) {
        if (beanProperty == null) {
            return new Annotation[0];
        }
        return beanProperty.getMember().annotations().toArray(Annotation[]::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        Object substitute = deserializationContext.readValue(jsonParser, substituteType);
        return inputConverter.convertInput(substitute, detectedType, environment, valueMapper);
    }

    private JavaType extractType(Annotated annotated) {
        if (annotated instanceof AnnotatedMethod) {
            AnnotatedMethod method = (AnnotatedMethod) annotated;
            if (ClassUtils.isSetter(method.getAnnotated())) {
                return method.getParameterType(0);
            }
            return method.getType();
        }
        return annotated.getType();
    }

    private static class DefaultDeserializer extends ValueDeserializer {

        private final JavaType javaType;

        DefaultDeserializer(JavaType javaType) {
            this.javaType = javaType;
        }

        @Override
        public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            return deserializationContext.readValue(jsonParser, javaType);
        }
    }
}
