package io.leangen.graphql.util;

import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.simple.ScalarOnlyValueMapperFactory;

public class Defaults {

    private enum JsonLib {
        JACKSON("com.fasterxml.jackson.databind.ObjectMapper"), GSON("com.google.gson.Gson"), SCALAR("java.lang.Object");
        
        public final String requiredClass;
        
        JsonLib(String requiredClass) {
            this.requiredClass = requiredClass;
        }
    }
    
    private static JsonLib jsonLibrary() {
        for (JsonLib jsonLib : JsonLib.values()) {
            try {
                ClassUtils.forName(jsonLib.requiredClass);
                return jsonLib;
            } catch (ClassNotFoundException ge) {/*no-op*/}
        }
        throw new IllegalStateException("No JSON deserialization library found on classpath");
    }
    
    public static ValueMapperFactory valueMapperFactory(String basePackage, TypeInfoGenerator typeInfoGenerator) {
        switch (jsonLibrary()) {
            case GSON: return new GsonValueMapperFactory(basePackage, typeInfoGenerator);
            case JACKSON: return new JacksonValueMapperFactory(basePackage, typeInfoGenerator);
            default: return new ScalarOnlyValueMapperFactory();
        }
    }
}
