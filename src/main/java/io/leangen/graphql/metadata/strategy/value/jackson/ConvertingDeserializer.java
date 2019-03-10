package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;

public class ConvertingDeserializer extends JsonDeserializer implements ContextualDeserializer {

    private final AnnotatedType detectedType;
    private final JavaType substituteType;
    private final InputConverter inputConverter;
    private final GlobalEnvironment environment;
    private final ValueMapper valueMapper;

    public ConvertingDeserializer(InputConverter inputConverter, GlobalEnvironment environment) {
        this.detectedType = null;
        this.substituteType = null;
        this.inputConverter = inputConverter;
        this.environment = environment;
        this.valueMapper = null;
    }

    private ConvertingDeserializer(AnnotatedType detectedType, JavaType substituteType, InputConverter inputConverter, GlobalEnvironment environment, ObjectMapper objectMapper) {
        this.detectedType = detectedType;
        this.substituteType = substituteType;
        this.inputConverter = inputConverter;
        this.environment = environment;
        this.valueMapper = new JacksonValueMapper(objectMapper);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) {
        JavaType javaType = deserializationContext.getContextualType() != null ? deserializationContext.getContextualType() : extractType(beanProperty.getMember());
        Annotation[] annotations = annotations(beanProperty);
        AnnotatedType detectedType = environment.typeTransformer.transform(ClassUtils.addAnnotations(TypeUtils.toJavaType(javaType), annotations));
        JavaType substituteType = deserializationContext.getTypeFactory().constructType(environment.getMappableInputType(detectedType).getType());
        if (inputConverter.supports(detectedType)) {
            return new ConvertingDeserializer(detectedType, substituteType, inputConverter, environment, (ObjectMapper) deserializationContext.getParser().getCodec());
        } else {
            return new DefaultDeserializer(javaType);
        }
    }

    private Annotation[] annotations(BeanProperty beanProperty) {
        if (beanProperty == null) {
            return new Annotation[0];
        }
        List<Annotation> annotations = new ArrayList<>();
        beanProperty.getMember().getAllAnnotations().annotations().forEach(annotations::add);
        return annotations.toArray(new Annotation[0]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
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

    private static class DefaultDeserializer extends JsonDeserializer {

        private final JavaType javaType;

        DefaultDeserializer(JavaType javaType) {
            this.javaType = javaType;
        }

        @Override
        public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            return deserializationContext.readValue(jsonParser, javaType);
        }
    }
}
