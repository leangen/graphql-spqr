package io.leangen.graphql.util;

import io.leangen.graphql.metadata.strategy.type.TypeMetaDataGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.simple.ScalarOnlyValueMapperFactory;

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
    
    public static ValueMapperFactory valueMapperFactory(TypeMetaDataGenerator metaDataGenerator) {
        switch (jsonLibrary()) {
            case GSON: return new GsonValueMapperFactory(metaDataGenerator);
            case JACKSON: return new JacksonValueMapperFactory(metaDataGenerator);
            default: return new ScalarOnlyValueMapperFactory();
        }
    }
}
