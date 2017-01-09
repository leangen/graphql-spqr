package io.leangen.graphql.metadata.strategy.input;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public class GsonInputDeserializer implements InputDeserializer {

    private final Gson gson;

    public GsonInputDeserializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Object graphQlInput, Type sourceType, AnnotatedType outputType) {
        if (graphQlInput.getClass() == outputType.getType()) {
            return (T) graphQlInput;
        }
        JsonElement jsonElement = gson.toJsonTree(graphQlInput, sourceType);
        return gson.fromJson(jsonElement, outputType.getType());
    }

    @Override
    public <T> T deserializeString(String json, AnnotatedType outputType) {
        return gson.fromJson(json, outputType.getType());
    }
}
