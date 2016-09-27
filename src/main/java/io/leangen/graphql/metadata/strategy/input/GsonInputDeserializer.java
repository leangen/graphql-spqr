package io.leangen.graphql.metadata.strategy.input;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public class GsonInputDeserializer implements InputDeserializer {

    private Gson gson;

    public GsonInputDeserializer() {
        this(new GsonFieldNamingStrategy());
    }

    public GsonInputDeserializer(FieldNamingStrategy fieldNamingStrategy) {
        this.gson = new GsonBuilder()
                .setFieldNamingStrategy(fieldNamingStrategy)
                .create();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Object graphQlInput, AnnotatedType type) {
        if (graphQlInput.getClass() == type.getType()) {
            return (T) graphQlInput;
        }
        JsonElement jsonElement = gson.toJsonTree(graphQlInput);
        return gson.fromJson(jsonElement, type.getType());
    }
}
