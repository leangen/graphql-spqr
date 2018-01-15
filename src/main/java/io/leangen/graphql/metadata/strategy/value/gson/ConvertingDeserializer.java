package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

class ConvertingDeserializer implements JsonDeserializer {

    private final InputConverter inputConverter;
    private final GlobalEnvironment environment;
    private final ValueMapper valueMapper;

    ConvertingDeserializer(InputConverter inputConverter, GlobalEnvironment environment, Gson gson) {
        this.inputConverter = inputConverter;
        this.environment = environment;
        this.valueMapper = new GsonValueMapper(gson);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext deserializationContext) throws JsonParseException {
        AnnotatedType detectedType = GenericTypeReflector.annotate(type);
        Type substituteType = environment.getMappableType(detectedType).getType();
        Object substitute = deserializationContext.deserialize(jsonElement, substituteType);
        return inputConverter.convertInput(substitute, detectedType, environment, valueMapper);
    }
}
