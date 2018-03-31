package io.leangen.graphql.metadata.strategy.value.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.value.InputFieldDiscoveryStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class GsonValueMapper implements ValueMapper, InputFieldDiscoveryStrategy {

    private final Gson gson;

    public GsonValueMapper(Gson gson) {
        this.gson = gson;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) {
        if (graphQLInput.getClass() == outputType.getType()) {
            return (T) graphQLInput;
        }
        JsonElement jsonElement = gson.toJsonTree(graphQLInput, sourceType);
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

    /**
     * Unlike Jackson, Gson doesn't expose any of its metadata, so this method is more or less a
     * reimplementation of {@link com.google.gson.internal.bind.ReflectiveTypeAdapterFactory#getBoundFields(Gson, com.google.gson.reflect.TypeToken, Class)}
     * @param type Java type (used as query input) to be analyzed for deserializable fields
     * @return All deserializable fields that could be discovered from this {@link AnnotatedType}
     */
    @Override
    public Set<InputField> getInputFields(AnnotatedType type, InclusionStrategy inclusionStrategy) {
        Set<InputField> inputFields = new HashSet<>();
        Class<?> raw = ClassUtils.getRawType(type.getType());
        if (raw.isInterface() || raw.isPrimitive()) {
            return inputFields;
        }

        while (raw != Object.class) {
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                if (gson.excluder().excludeClass(field.getType(), false)
                        || gson.excluder().excludeField(field, false)) {
                    continue;
                }
                AnnotatedType fieldType = ClassUtils.getFieldType(field, type);
                Optional<Method> setter = ClassUtils.findSetter(field.getDeclaringClass(), field.getName(), field.getType());
                Member target = setter.isPresent() ? setter.get() : field;
                if (!inclusionStrategy.includeInputField(target.getDeclaringClass(), (AnnotatedElement) target, fieldType)) {
                    continue;
                }
                field.setAccessible(true);
                String fieldName = gson.fieldNamingStrategy().translateName(field);
                if (!inputFields.add(new InputField(fieldName, null, fieldType, null))) {
                    throw new IllegalArgumentException(raw + " declares multiple input fields named " + fieldName);
                }
            }
            raw = raw.getSuperclass();
            type = GenericTypeReflector.getExactSuperType(type, raw);
        }
        return inputFields;
    }
}
