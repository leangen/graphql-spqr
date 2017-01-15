package io.leangen.graphql.util;

import io.leangen.graphql.metadata.strategy.input.GsonInputDeserializerFactory;
import io.leangen.graphql.metadata.strategy.input.InputDeserializerFactory;
import io.leangen.graphql.metadata.strategy.input.JacksonInputDeserializerFactory;
import io.leangen.graphql.metadata.strategy.input.ScalarOnlyInputDeserializerFactory;

public class Defaults {

    private enum JsonLib {
        GSON("com.google.gson.Gson"), JACKSON("com.fasterxml.jackson.databind.ObjectMapper"), SCALAR("java.lang.Object");
        
        public final String requiredClass;
        
        JsonLib(String requiredClass) {
            this.requiredClass = requiredClass;
        }
    }
    
    private static JsonLib jsonLibrary() {
        for (JsonLib jsonLib : JsonLib.values()) {
            try {
                Class.forName(jsonLib.requiredClass);
                return jsonLib;
            } catch (ClassNotFoundException ge) {/*no-op*/}
        }
        throw new IllegalStateException("No JSON deserialization library found on classpath");
    }
    
    public static InputDeserializerFactory inputDeserializerFactory() {
        switch (jsonLibrary()) {
            case GSON: return new GsonInputDeserializerFactory();
            case JACKSON: return new JacksonInputDeserializerFactory();
            default: return new ScalarOnlyInputDeserializerFactory();
        }
    }
}
