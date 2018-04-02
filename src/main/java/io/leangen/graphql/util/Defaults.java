package io.leangen.graphql.util;

import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;

public class Defaults {

    private static final IllegalStateException noJsonLib = new IllegalStateException(
            "No JSON deserialization library found on classpath. A compatible version of either Jackson or Gson "
                    + "must be available or a custom ValueMapperFactory must be provided");

    private enum JsonLib {
        JACKSON("com.fasterxml.jackson.databind.ObjectMapper"), GSON("com.google.gson.Gson");

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
        throw noJsonLib;
    }

    public static ValueMapperFactory valueMapperFactory(TypeInfoGenerator typeInfoGenerator) {
        switch (jsonLibrary()) {
            case GSON: return GsonValueMapperFactory.builder()
                    .withTypeInfoGenerator(typeInfoGenerator)
                    .build();
            case JACKSON: return JacksonValueMapperFactory.builder()
                    .withTypeInfoGenerator(typeInfoGenerator)
                    .build();
            default: throw noJsonLib;
        }
    }
}
