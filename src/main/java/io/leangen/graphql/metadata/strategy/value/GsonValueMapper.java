package io.leangen.graphql.metadata.strategy.value;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public class GsonValueMapper implements ValueMapper {

    private final Gson gson;

    public GsonValueMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromInput(Object graphQlInput, Type sourceType, AnnotatedType outputType) {
        if (graphQlInput.getClass() == outputType.getType()) {
            return (T) graphQlInput;
        }
        JsonElement jsonElement = gson.toJsonTree(graphQlInput, sourceType);
        return gson.fromJson(jsonElement, outputType.getType());
    }

    @Override
    public <T> T fromString(String json, AnnotatedType outputType) {
        return gson.fromJson(json, outputType.getType());
    }

    @Override
    public String toString(Object output) {
        return gson.toJson(output);
    }
}
