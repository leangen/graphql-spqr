package io.leangen.graphql.util;

import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;
import io.leangen.graphql.metadata.strategy.value.gson.GsonValueMapperFactory;
import io.leangen.graphql.module.Module;
import io.leangen.graphql.module.common.gson.GsonModule;

import java.util.ArrayList;
import java.util.List;

public class Defaults {

    private static final IllegalStateException noJsonLib = new IllegalStateException(
            "No JSON deserialization library found on classpath. A compatible version of either Jackson or Gson "
                    + "must be available or a custom ValueMapperFactory must be provided");

    /**
     * JSON libraries supported by the built-in defaults. Jackson 3 is preferred when
     * more than one supported library is present, so that implicit defaults remain
     * deterministic during a Jackson 2 to 3 migration.
     */
    public enum JsonLibrary {
        JACKSON_3("tools.jackson.databind.ObjectMapper"),
        JACKSON_2("com.fasterxml.jackson.databind.ObjectMapper"),
        GSON("com.google.gson.Gson");

        public final String requiredClass;

        JsonLibrary(String requiredClass) {
            this.requiredClass = requiredClass;
        }
    }

    private static JsonLibrary jsonLibrary() {
        for (JsonLibrary jsonLib : JsonLibrary.values()) {
            if (isAvailable(jsonLib)) {
                return jsonLib;
            }
        }
        throw noJsonLib;
    }

    private static boolean isAvailable(JsonLibrary jsonLib) {
        try {
            ClassUtils.forName(jsonLib.requiredClass);
            return true;
        } catch (ClassNotFoundException ge) {
            return false;
        }
    }

    public static ValueMapperFactory<?> valueMapperFactory() {
        return valueMapperFactory(jsonLibrary());
    }

    public static ValueMapperFactory<?> valueMapperFactory(JsonLibrary jsonLibrary) {
        switch (jsonLibrary) {
            case GSON: return GsonValueMapperFactory.builder().build();
            case JACKSON_2: return io.leangen.graphql.metadata.strategy.value.jackson2.JacksonValueMapperFactory.builder().build();
            case JACKSON_3: return io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory.builder().build();
            default: throw noJsonLib;
        }
    }

    public static ValueMapperFactory<?> valueMapperFactory(TypeInfoGenerator typeInfoGenerator) {
        return valueMapperFactory(jsonLibrary(), typeInfoGenerator);
    }

    public static ValueMapperFactory<?> valueMapperFactory(JsonLibrary jsonLibrary, TypeInfoGenerator typeInfoGenerator) {
        switch (jsonLibrary) {
            case GSON: return GsonValueMapperFactory.builder()
                    .withTypeInfoGenerator(typeInfoGenerator)
                    .build();
            case JACKSON_2: return io.leangen.graphql.metadata.strategy.value.jackson2.JacksonValueMapperFactory.builder()
                    .withTypeInfoGenerator(typeInfoGenerator)
                    .build();
            case JACKSON_3: return io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory.builder()
                    .withTypeInfoGenerator(typeInfoGenerator)
                    .build();
            default: throw noJsonLib;
        }
    }

    public static List<Module> modules() {
        return modules(jsonLibrary());
    }

    public static List<Module> modules(JsonLibrary jsonLibrary) {
        List<Module> defaultModules = new ArrayList<>(2);
        switch (jsonLibrary) {
            case JACKSON_3:
                defaultModules.add(new io.leangen.graphql.module.common.jackson.JacksonModule());
                break;
            case JACKSON_2:
                defaultModules.add(new io.leangen.graphql.module.common.jackson2.JacksonModule());
                break;
            case GSON:
                defaultModules.add(new GsonModule());
                break;
            default:
                throw noJsonLib;
        }
        if (jsonLibrary != JsonLibrary.GSON && isAvailable(JsonLibrary.GSON)) {
            defaultModules.add(new GsonModule());
        }
        return defaultModules;
    }
}
