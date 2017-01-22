package io.leangen.graphql.util;

import io.leangen.graphql.metadata.strategy.value.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.JacksonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.ScalarOnlyValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

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
    
    public static ValueMapperFactory valueMapperFactory() {
        switch (jsonLibrary()) {
            case GSON: return new GsonValueMapperFactory();
            case JACKSON: return new JacksonValueMapperFactory();
            default: return new ScalarOnlyValueMapperFactory();
        }
    }
}
