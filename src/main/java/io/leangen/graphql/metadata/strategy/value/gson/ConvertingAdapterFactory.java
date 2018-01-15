package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.AnnotatedType;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.GlobalEnvironment;

class ConvertingAdapterFactory implements TypeAdapterFactory {

    private final GlobalEnvironment environment;

    ConvertingAdapterFactory(GlobalEnvironment environment) {
        this.environment = environment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        AnnotatedType detectedType = GenericTypeReflector.annotate(type.getType());
        return environment.getInputConverters().stream()
                .filter(converter -> converter.supports(detectedType)).findFirst()
                .map(converter ->  new ConvertingDeserializer(converter, environment, gson))
                .map(deserializer -> new TreeTypeAdapter(null, deserializer, gson, type, this))
                .orElse(null);
    }
}
